import type { Pool, PoolClient } from 'pg';
import type { EditCandidate, ReviewQuery } from './vocabulary-review.schema.js';
import type { ReviewCandidateRow, ReviewPage } from './vocabulary-review.types.js';

const columns = `c.candidate_id,c.english,c.vietnamese,c.pronunciation,c.part_of_speech,c.cefr,
 c.primary_topic_code,c.topic_codes,c.example,c.example_vi,c.source_code,c.source_name,c.source_url,
 c.source_version,c.source_license,c.source_record_id,c.validation_reasons,c.quality_score,c.quality_verified,
 c.license_status,c.status,c.reviewed_by,u.username AS reviewer_username,c.reviewed_at,
 c.published_flashcard_id,c.row_version,c.updated_at,
 (SELECT f.id FROM flashcards f WHERE lower(f.english)=lower(c.english)
   AND lower(f.vietnamese)=lower(c.vietnamese) ORDER BY f.id LIMIT 1) AS exact_duplicate_id,
 (SELECT count(*)::int FROM flashcards f WHERE lower(f.english)=lower(c.english)) AS same_word_count`;
const literalSearch=(v:string)=>`%${v.replace(/[\\%_]/g,ch=>'\\'+ch)}%`;
const snapshot = (row:Record<string,unknown>|undefined) => row ? JSON.parse(JSON.stringify(row)) : null;

export function createVocabularyReviewRepository(pool:Pool) {
  async function tx<T>(work:(client:PoolClient)=>Promise<T>) { const c=await pool.connect(); try { await c.query('BEGIN'); const out=await work(c); await c.query('COMMIT'); return out; } catch(e){await c.query('ROLLBACK'); throw e;} finally{c.release();} }
  async function get(client:Pool|PoolClient,id:string,lock=false) {
    return (await client.query<ReviewCandidateRow>(`SELECT ${columns} FROM vocabulary_review_candidates c
      LEFT JOIN users u ON u.id=c.reviewed_by WHERE c.candidate_id=$1 ${lock?'FOR UPDATE OF c':''}`,[id])).rows[0];
  }
  async function audit(client:PoolClient,actor:number,id:string,action:string,before:unknown,after:unknown,batchId:string|null=null) {
    await client.query(`INSERT INTO vocabulary_review_audit(candidate_id,actor_user_id,action,before_value,after_value,batch_id)
      VALUES($1,$2,$3,$4::jsonb,$5::jsonb,$6)`,[id,actor,action,JSON.stringify(before),JSON.stringify(after),batchId]);
  }
  return {
    async stats() {
      const r=await pool.query(`SELECT count(*)::int AS total,count(*) FILTER(WHERE status='PENDING')::int AS pending,
        count(*) FILTER(WHERE status='APPROVED')::int AS approved,count(*) FILTER(WHERE status='REJECTED')::int AS rejected,
        count(*) FILTER(WHERE status='PUBLISHED')::int AS published FROM vocabulary_review_candidates`);
      return r.rows[0];
    },
    async filters() {
      const [topics,pos,reasons,sources]=await Promise.all([
        pool.query(`SELECT DISTINCT unnest(topic_codes) value FROM vocabulary_review_candidates ORDER BY value`),
        pool.query(`SELECT DISTINCT part_of_speech value FROM vocabulary_review_candidates WHERE part_of_speech IS NOT NULL ORDER BY value`),
        pool.query(`SELECT DISTINCT unnest(validation_reasons) value FROM vocabulary_review_candidates ORDER BY value`),
        pool.query(`SELECT DISTINCT source_code value,source_name label FROM vocabulary_review_candidates ORDER BY label`),
      ]); return {topics:topics.rows.map(r=>r.value),parts:pos.rows.map(r=>r.value),reasons:reasons.rows.map(r=>r.value),sources:sources.rows};
    },
    async list(q:ReviewQuery):Promise<ReviewPage> {
      const params=[literalSearch(q.search),q.level??null,q.topicCode??null,q.partOfSpeech??null,q.status??null,q.reason??null,q.source??null];
      const where=`WHERE (c.english ILIKE $1 ESCAPE '\\' OR c.vietnamese ILIKE $1 ESCAPE '\\')
       AND ($2::text IS NULL OR c.cefr=$2) AND ($3::text IS NULL OR $3=ANY(c.topic_codes))
       AND ($4::text IS NULL OR c.part_of_speech=$4) AND ($5::text IS NULL OR c.status=$5)
       AND ($6::text IS NULL OR $6=ANY(c.validation_reasons)) AND ($7::text IS NULL OR c.source_code=$7)`;
      const [count,rows]=await Promise.all([
        pool.query<{total:string}>(`SELECT count(*)::text total FROM vocabulary_review_candidates c ${where}`,params),
        pool.query<ReviewCandidateRow>(`SELECT ${columns} FROM vocabulary_review_candidates c LEFT JOIN users u ON u.id=c.reviewed_by
          ${where} ORDER BY c.candidate_id LIMIT $8 OFFSET $9`,[...params,q.limit,(q.page-1)*q.limit]),
      ]); return {rows:rows.rows,total:count.rows[0]!.total};
    },
    async one(id:string){return get(pool,id);},
    async edit(actor:number,id:string,input:EditCandidate) { return tx(async client=>{
      const before=await get(client,id,true); if(!before) return undefined;
      const saved=(await client.query(`UPDATE vocabulary_review_candidates SET english=$2,vietnamese=$3,pronunciation=$4,
        part_of_speech=$5,cefr=$6,primary_topic_code=$7,topic_codes=$8,example=$9,example_vi=$10,
        status='PENDING',quality_verified=FALSE,reviewed_by=$11,reviewed_at=CURRENT_TIMESTAMP,
        row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE candidate_id=$1 AND row_version=$12 RETURNING *`,
        [id,input.english,input.vietnamese,input.pronunciation,input.partOfSpeech,input.cefr,input.primaryTopicCode,
          input.topicCodes,input.example,input.exampleVi,actor,input.rowVersion])).rows[0];
      if(!saved) return null; await audit(client,actor,id,'EDIT',snapshot(before as unknown as Record<string,unknown>),saved); return get(client,id);
    });},
    async changeStatus(actor:number,id:string,status:'PENDING'|'APPROVED'|'REJECTED',version:number,bulk=false) { return tx(async client=>{
      const before=await get(client,id,true); if(!before) return undefined;
      const saved=(await client.query(`UPDATE vocabulary_review_candidates SET status=$2,quality_verified=($2='APPROVED'),
        reviewed_by=$3,reviewed_at=CURRENT_TIMESTAMP,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP
        WHERE candidate_id=$1 AND row_version=$4 AND status<>'PUBLISHED' RETURNING *`,[id,status,actor,version])).rows[0];
      if(!saved) return null; const verb=status==='PENDING'?'RESET_PENDING':status==='APPROVED'?'APPROVE':'REJECT';
      const action=bulk?`BULK_${verb}`:verb;
      await audit(client,actor,id,action,snapshot(before as unknown as Record<string,unknown>),saved); return get(client,id);
    });},
    async bulkChangeStatus(actor:number,ids:string[],status:'PENDING'|'APPROVED'|'REJECTED') { return tx(async client=>{
      const rows=(await client.query<ReviewCandidateRow>(`SELECT ${columns} FROM vocabulary_review_candidates c
        LEFT JOIN users u ON u.id=c.reviewed_by WHERE c.candidate_id=ANY($1::text[]) ORDER BY c.candidate_id FOR UPDATE OF c`,[ids])).rows;
      if(rows.length!==ids.length) return undefined;
      if(rows.some(row=>row.status==='PUBLISHED')) return null;
      for(const row of rows) {
        const saved=(await client.query(`UPDATE vocabulary_review_candidates SET status=$2,quality_verified=($2='APPROVED'),
          reviewed_by=$3,reviewed_at=CURRENT_TIMESTAMP,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP
          WHERE candidate_id=$1 RETURNING *`,[row.candidate_id,status,actor])).rows[0];
        const action=`BULK_${status==='PENDING'?'RESET_PENDING':status==='APPROVED'?'APPROVE':'REJECT'}`;
        await audit(client,actor,row.candidate_id,action,snapshot(row as unknown as Record<string,unknown>),saved);
      }
      return rows.length;
    });},
    async batchRows(ids:string[]) { return (await pool.query<ReviewCandidateRow>(`SELECT ${columns} FROM vocabulary_review_candidates c
      LEFT JOIN users u ON u.id=c.reviewed_by WHERE c.candidate_id=ANY($1::text[]) ORDER BY c.candidate_id`,[ids])).rows; },
    async savePreview(actor:number,batch:{id:string;fingerprint:string;preview:unknown;items:Array<{id:string;version:number;disposition:string;duplicateId:number|null;reason:string|null}>}) {
      return tx(async client=>{
        const existing=(await client.query(`SELECT batch_id,status,preview,inserted_count,skipped_count,blocked_count
          FROM vocabulary_publish_batches WHERE fingerprint=$1`,[batch.fingerprint])).rows[0];
        if(existing) return existing;
        await client.query(`INSERT INTO vocabulary_publish_batches(batch_id,fingerprint,status,created_by,preview,blocked_count)
          VALUES($1,$2,'PREVIEWED',$3,$4::jsonb,$5)`,[batch.id,batch.fingerprint,actor,JSON.stringify(batch.preview),batch.items.filter(i=>i.disposition==='BLOCKED').length]);
        for(const item of batch.items) {
          await client.query(`INSERT INTO vocabulary_publish_batch_items
            (batch_id,candidate_id,candidate_version,disposition,duplicate_flashcard_id,reason) VALUES($1,$2,$3,$4,$5,$6)`,
          [batch.id,item.id,item.version,item.disposition,item.duplicateId,item.reason]);
          await audit(client,actor,item.id,'PREVIEW',null,{disposition:item.disposition,reason:item.reason},batch.id);
        }
        return (await client.query(`SELECT batch_id,status,preview,inserted_count,skipped_count,blocked_count
          FROM vocabulary_publish_batches WHERE batch_id=$1`,[batch.id])).rows[0];
      });
    },
    async publish(actor:number,batchId:string) { return tx(async client=>{
      const batch=(await client.query(`SELECT * FROM vocabulary_publish_batches WHERE batch_id=$1 FOR UPDATE`,[batchId])).rows[0];
      if(!batch) return undefined;
      if(batch.status==='COMPLETED') return batch;
      if(batch.status!=='PREVIEWED') return null;
      if(batch.created_by!==actor) return {forbidden:true};
      const items=(await client.query(`SELECT i.*,c.* FROM vocabulary_publish_batch_items i
        JOIN vocabulary_review_candidates c ON c.candidate_id=i.candidate_id
        WHERE i.batch_id=$1 ORDER BY i.candidate_id FOR UPDATE OF c`,[batchId])).rows;
      if(items.some(i=>i.disposition==='BLOCKED')) return {blocked:true};
      await client.query(`UPDATE vocabulary_publish_batches SET status='PUBLISHING',confirmed_by=$2,
        confirmed_at=CURRENT_TIMESTAMP WHERE batch_id=$1`,[batchId,actor]);
      let inserted=0,skipped=0;
      for(const item of items) {
        if(item.status!=='APPROVED'||item.row_version!==item.candidate_version||!item.quality_verified||item.license_status==='BLOCKED') {
          throw Object.assign(new Error('Candidate changed after preview'),{code:'M13_REVIEW_STALE'});
        }
        let flashcardId:number;
        const exact=(await client.query<{id:number}>(`SELECT id FROM flashcards WHERE lower(english)=lower($1)
          AND lower(vietnamese)=lower($2) ORDER BY id LIMIT 1`,[item.english,item.vietnamese])).rows[0];
        if(exact) { flashcardId=exact.id; skipped+=1; }
        else {
          const codes=[...new Set(item.topic_codes as string[])];
          for(const code of codes) await client.query(`INSERT INTO topics(code,name,description,image_url)
            SELECT code,name,description,NULL FROM vocabulary_review_topics WHERE code=$1
            ON CONFLICT(code) DO NOTHING`,[code]);
          const topicRows=await client.query<{id:number;code:string}>('SELECT id,code FROM topics WHERE code=ANY($1::text[])',[codes]);
          if(topicRows.rowCount!==codes.length) throw new Error(`Missing reviewed topic for ${item.candidate_id}`);
          const topicMap=new Map(topicRows.rows.map(t=>[t.code,t.id]));
          const primary=topicMap.get(item.primary_topic_code); if(!primary) throw new Error('Missing primary topic');
          const card=(await client.query<{id:number}>(`INSERT INTO flashcards(topic_id,english,vietnamese,pronunciation,
            part_of_speech,example,example_vi,level,is_active) VALUES($1,$2,$3,$4,$5,$6,$7,$8,TRUE) RETURNING id`,
          [primary,item.english,item.vietnamese,item.pronunciation,item.part_of_speech,item.example,item.example_vi,item.cefr])).rows[0]!;
          flashcardId=card.id; inserted+=1;
          for(const [code,topicId] of topicMap) await client.query(`INSERT INTO flashcard_topics(flashcard_id,topic_id,is_primary)
            VALUES($1,$2,$3) ON CONFLICT(flashcard_id,topic_id) DO UPDATE SET is_primary=EXCLUDED.is_primary`,
          [flashcardId,topicId,code===item.primary_topic_code]);
        }
        const requiredSources=item.cefr?['WORDS_CEFR_2024',item.source_code]:[item.source_code];
        const sources=await client.query<{id:number;code:string}>('SELECT id,code FROM vocabulary_sources WHERE code=ANY($1::text[])',
          [requiredSources]);
        const sourceMap=new Map(sources.rows.map(s=>[s.code,s.id]));
        if(sources.rowCount!==new Set(requiredSources).size||!sourceMap.has(item.source_code)) throw new Error('Vocabulary source metadata missing');
        const validationStatus=item.original_payload?.validationStatus==='APPROVED_AUTO'?'APPROVED_AUTO':'APPROVED_MANUAL';
        const quizEligible=item.original_payload?.quizEligible===true;
        const provenance=[
          ...(item.cefr?[[sourceMap.get('WORDS_CEFR_2024'),`${item.candidate_id}:cefr`,null,null]]:[]),
          [sourceMap.get(item.source_code),item.source_record_id,item.source_word_id,item.source_definition_id],
        ];
        for(const p of provenance) await client.query(`INSERT INTO flashcard_provenance
          (flashcard_id,source_id,source_record_id,source_word_id,source_definition_id,cefr_scope,validation_status,validation_reasons,quiz_eligible)
          VALUES($1,$2,$3,$4,$5,$6,$7,$8::jsonb,$9) ON CONFLICT DO NOTHING`,
        [flashcardId,...p,item.cefr_scope,validationStatus,JSON.stringify(item.validation_reasons),quizEligible]);
        const before={status:item.status,rowVersion:item.row_version};
        const saved=(await client.query(`UPDATE vocabulary_review_candidates SET status='PUBLISHED',published_flashcard_id=$2,
          reviewed_by=$3,reviewed_at=CURRENT_TIMESTAMP,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP
          WHERE candidate_id=$1 RETURNING status,published_flashcard_id,row_version`,[item.candidate_id,flashcardId,actor])).rows[0];
        await audit(client,actor,item.candidate_id,exact?'PUBLISH_SKIPPED':'PUBLISH',before,saved,batchId);
      }
      return (await client.query(`UPDATE vocabulary_publish_batches SET status='COMPLETED',inserted_count=$2,
        skipped_count=$3,blocked_count=0,completed_at=CURRENT_TIMESTAMP WHERE batch_id=$1 RETURNING *`,
      [batchId,inserted,skipped])).rows[0];
    });},
    async auditPage(candidateId:string,page:number,limit:number) {
      const [count,rows]=await Promise.all([
        pool.query<{total:string}>('SELECT count(*)::text total FROM vocabulary_review_audit WHERE candidate_id=$1',[candidateId]),
        pool.query(`SELECT a.audit_id,a.action,a.before_value,a.after_value,a.batch_id,a.created_at,u.username actor
          FROM vocabulary_review_audit a JOIN users u ON u.id=a.actor_user_id WHERE a.candidate_id=$1
          ORDER BY a.created_at DESC,a.audit_id DESC LIMIT $2 OFFSET $3`,[candidateId,limit,(page-1)*limit]),
      ]); return {rows:rows.rows,total:count.rows[0]!.total};
    },
    pool,
    audit,
    get,
    tx,
  };
}
export type VocabularyReviewRepository=ReturnType<typeof createVocabularyReviewRepository>;

