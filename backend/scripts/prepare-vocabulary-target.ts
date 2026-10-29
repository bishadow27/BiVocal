import 'dotenv/config';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

type Candidate = {
  candidateId:string; headword:string; vietnamese:string; pronunciation:string|null;
  partOfSpeech:string|null; cefr:string|null; primaryTopicCode:string|null;
  topicCodes:string[]; example:string|null; exampleVi:string|null;
  validationReasons:string[]; validationStatus:'APPROVED_AUTO'|'NEEDS_REVIEW'|'REJECTED';
  qualityScore:number; cefrScope:string; source:Record<string,unknown>;
};

const args=process.argv.slice(2),apply=args.includes('--apply');
const value=(name:string)=>{const i=args.indexOf(name);return i>=0?args[i+1]:undefined;};
const target=Number(value('--target-total')??'5000'),confirmation=value('--database-confirm');
const inputDir=value('--input-dir')??'processed';
if(!Number.isSafeInteger(target)||target<358)throw new Error('Invalid --target-total');
const env=readDatabaseEnv(process.env);
if(apply&&(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)||confirmation!==env.DB_NAME))
  throw new Error('Apply requires local PostgreSQL and exact --database-confirm');

const datasetRoot=fileURLToPath(new URL('../datasets/',import.meta.url));
const inputUrl=path.join(datasetRoot,inputDir,inputDir==='processed'?'bivoca_vocab_candidates.jsonl':'candidates.jsonl');
const text=await readFile(inputUrl,'utf8');
const candidates=text.split(/\r?\n/).filter(Boolean).map(line=>{
  const candidate=JSON.parse(line) as Candidate & {normalizedKey?:string};
  // PostgreSQL jsonb rejects U+0000 used by the reproducible local lookup key.
  delete candidate.normalizedKey;
  return candidate;
});
const checksum=createHash('sha256').update(text).digest('hex');
const structurallyApproved=candidates.filter(c=>c.validationStatus==='APPROVED_AUTO');
// APPROVED_AUTO is only structural preflight. Publication requires explicit, source-backed
// semantic verification; VNEDICT substring overlap intentionally does not satisfy this guard.
const approvedPool=structurallyApproved.filter(c=>c.source.semanticVerified===true).sort((a,b)=>{
  const ai=Number(a.source.semanticVerified===true),bi=Number(b.source.semanticVerified===true);
  if(ai!==bi)return bi-ai;
  const ac=Number(Boolean(a.cefr)),bc=Number(Boolean(b.cefr));if(ac!==bc)return bc-ac;
  if(a.qualityScore!==b.qualityScore)return b.qualityScore-a.qualityScore;
  return a.candidateId.localeCompare(b.candidateId);
});
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:2,connectionTimeoutMillis:5000});

try{
  const database=(await pool.query<{name:string}>('SELECT current_database() name')).rows[0]!.name;
  if(database!==env.DB_NAME)throw new Error('Connected database mismatch');
  const current=(await pool.query<{cards:number;candidates:number}>(`SELECT
    (SELECT count(*)::int FROM flashcards) cards,
    (SELECT count(*)::int FROM vocabulary_review_candidates) candidates`)).rows[0]!;
  const published=new Set((await pool.query<{candidate_id:string}>(
    `SELECT candidate_id FROM vocabulary_review_candidates WHERE status='PUBLISHED'`)).rows.map(r=>r.candidate_id));
  const existingSenses=new Set((await pool.query<{key:string}>(`SELECT lower(trim(english))||chr(31)||
    lower(trim(vietnamese)) key FROM flashcards`)).rows.map(r=>r.key));
  const actor=(await pool.query<{id:number;username:string}>(`SELECT u.id,u.username FROM users u
    WHERE u.role='ADMIN' ORDER BY (SELECT count(*) FROM vocabulary_review_candidates c
      WHERE c.reviewed_by=u.id AND c.status='PUBLISHED') DESC,u.id LIMIT 1`)).rows[0];
  if(!actor)throw new Error('No ADMIN account is available for audit attribution');
  const need=Math.max(0,target-current.cards);
  const unpublished=approvedPool.filter(c=>!published.has(c.candidateId));
  const seenSenses=new Set(existingSenses);
  const available=unpublished.filter(c=>{
    const key=`${c.headword.trim().toLowerCase()}\u001f${c.vietnamese.trim().toLowerCase()}`;
    if(seenSenses.has(key))return false;seenSenses.add(key);return true;
  });
  if(available.length<need)throw new Error(`Only ${available.length} unpublished validated candidates are available; ${need} required`);
  const chosen=new Set(available.slice(0,need).map(c=>c.candidateId));
  const planned={mode:apply?'apply':'dry-run',database,currentCards:current.cards,target,needed:need,
    sourceCandidates:candidates.length,structuralPreflight:structurallyApproved.length,
    semanticallyVerifiedEligible:approvedPool.length,publishedCandidates:published.size,
    selectedForPublish:chosen.size,deferredAutoEligible:available.length-chosen.size,actorId:actor.id,
    exactDuplicatesExcluded:unpublished.length-available.length,inputSha256:checksum};
  if(!apply){console.log(JSON.stringify(planned,null,2));process.exit(0);}

  const client=await pool.connect();
  let updated=0,audited=0;
  try{
    await client.query('BEGIN');
    for(let offset=0;offset<candidates.length;offset+=250){
      const rows=candidates.slice(offset,offset+250).map(candidate=>{
        let targetStatus:'APPROVED'|'PENDING'|'REJECTED';
        if(chosen.has(candidate.candidateId))targetStatus='APPROVED';
        else if(candidate.validationStatus==='REJECTED')targetStatus='REJECTED';
        else targetStatus='PENDING';
        const reasons=[...candidate.validationReasons];
        if(candidate.validationStatus==='APPROVED_AUTO'&&targetStatus==='PENDING')reasons.push('DEFERRED_TARGET_REACHED');
        const deploymentSelection=chosen.has(candidate.candidateId)
          ?{targetTotal:target,inputSha256:checksum}:null;
        return {candidate:{...candidate,deploymentSelection},targetStatus,reasons:[...new Set(reasons)].sort()};
      });
      const result=await client.query<{changed:number;audits:number}>(`WITH incoming AS (
        SELECT value->'candidate' candidate,value->>'targetStatus' target_status,
          ARRAY(SELECT jsonb_array_elements_text(value->'reasons')) reasons
        FROM jsonb_array_elements($1::jsonb) value
      ), before AS MATERIALIZED (
        SELECT c.candidate_id,c.status old_status,i.* FROM vocabulary_review_candidates c
        JOIN incoming i ON i.candidate->>'candidateId'=c.candidate_id WHERE c.status<>'PUBLISHED' FOR UPDATE OF c
      ), changed AS (
        UPDATE vocabulary_review_candidates c SET
          english=b.candidate->>'headword',vietnamese=b.candidate->>'vietnamese',
          pronunciation=NULLIF(b.candidate->>'pronunciation',''),part_of_speech=NULLIF(b.candidate->>'partOfSpeech',''),
          cefr=NULLIF(b.candidate->>'cefr',''),primary_topic_code=NULLIF(b.candidate->>'primaryTopicCode',''),
          topic_codes=ARRAY(SELECT jsonb_array_elements_text(COALESCE(b.candidate->'topicCodes','[]'::jsonb))),
          example=NULLIF(b.candidate->>'example',''),example_vi=NULLIF(b.candidate->>'exampleVi',''),
          cefr_scope=b.candidate->>'cefrScope',validation_reasons=b.reasons,
          quality_score=COALESCE((b.candidate->>'qualityScore')::int,0),
          quality_verified=(b.target_status='APPROVED'),status=b.target_status,
          reviewed_by=$2,reviewed_at=CURRENT_TIMESTAMP,original_payload=b.candidate,
          row_version=c.row_version+1,updated_at=CURRENT_TIMESTAMP
        FROM before b WHERE c.candidate_id=b.candidate_id AND (
          c.english IS DISTINCT FROM b.candidate->>'headword' OR c.vietnamese IS DISTINCT FROM b.candidate->>'vietnamese'
          OR c.pronunciation IS DISTINCT FROM NULLIF(b.candidate->>'pronunciation','')
          OR c.part_of_speech IS DISTINCT FROM NULLIF(b.candidate->>'partOfSpeech','')
          OR c.cefr IS DISTINCT FROM NULLIF(b.candidate->>'cefr','')
          OR c.primary_topic_code IS DISTINCT FROM NULLIF(b.candidate->>'primaryTopicCode','')
          OR c.topic_codes IS DISTINCT FROM ARRAY(SELECT jsonb_array_elements_text(COALESCE(b.candidate->'topicCodes','[]'::jsonb)))
          OR c.example IS DISTINCT FROM NULLIF(b.candidate->>'example','') OR c.example_vi IS DISTINCT FROM NULLIF(b.candidate->>'exampleVi','')
          OR c.validation_reasons IS DISTINCT FROM b.reasons OR c.quality_score IS DISTINCT FROM COALESCE((b.candidate->>'qualityScore')::int,0)
          OR c.quality_verified IS DISTINCT FROM (b.target_status='APPROVED') OR c.status IS DISTINCT FROM b.target_status
          OR c.original_payload IS DISTINCT FROM b.candidate)
        RETURNING c.candidate_id,b.old_status,c.status new_status
      ), audit_rows AS (
        INSERT INTO vocabulary_review_audit(candidate_id,actor_user_id,action,before_value,after_value)
        SELECT candidate_id,$2,CASE new_status WHEN 'APPROVED' THEN 'BULK_APPROVE'
          WHEN 'REJECTED' THEN 'BULK_REJECT' ELSE 'BULK_RESET_PENDING' END,
          jsonb_build_object('status',old_status,'source','M13_7_AUTOMATED_QUALITY_GATE'),
          jsonb_build_object('status',new_status,'source','M13_7_AUTOMATED_QUALITY_GATE') FROM changed
        RETURNING 1
      ) SELECT (SELECT count(*)::int FROM changed) changed,(SELECT count(*)::int FROM audit_rows) audits`,
      [JSON.stringify(rows),actor.id]);
      updated+=result.rows[0]!.changed;audited+=result.rows[0]!.audits;
    }
    await client.query('COMMIT');
  }catch(error){await client.query('ROLLBACK');throw error;}finally{client.release();}
  console.log(JSON.stringify({...planned,updated,audited},null,2));
}finally{await pool.end();}
