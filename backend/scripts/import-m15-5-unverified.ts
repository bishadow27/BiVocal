import 'dotenv/config';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import pg, { type PoolClient } from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

type Candidate={candidateId:string;headword:string;vietnamese:string;pronunciation:string|null;partOfSpeech:string;
  cefr:string|null;cefrScope:string;example:string|null;exampleVi:string|null;primaryTopicCode:string;topicCodes:string[];
  validationReasons:string[];qualityScore:number;source:{name:string;url:string;version:string;license:string;wordId?:number;definitionId?:number};
  contentStatus:string;quizEligible:boolean};
const args=process.argv.slice(2),apply=args.includes('--apply');
const value=(name:string)=>{const index=args.indexOf(name);return index<0?undefined:args[index+1];};
const target=Number(value('--target-total')),confirmation=value('--database-confirm');
if(![10000,15000,20000].includes(target))throw new Error('--target-total must be 10000, 15000 or 20000');
const env=readDatabaseEnv(process.env);
if(apply&&(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)||confirmation!==env.DB_NAME))
  throw new Error('Apply requires local PostgreSQL and exact --database-confirm');
const planText=await readFile(new URL('../.m15-5-verification/m15-5-unverified-plan.jsonl',import.meta.url),'utf8');
const plan=planText.split(/\r?\n/).filter(Boolean).map(line=>JSON.parse(line) as Candidate);
if(plan.length!==15000)throw new Error(`Frozen plan must contain 15000 rows, found ${plan.length}`);
const planSha256=createHash('sha256').update(planText).digest('hex');
const prefix=plan.slice(0,target-5000);
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:2,statement_timeout:60000});
const sources=[
  ['WORDS_CEFR_2024','Words CEFR Dataset','local-sha256:c5e38190','https://github.com/Maximax67/Words-CEFR-Dataset','MIT','c5e381905a87b47bd1321ac1cd34f870b107c0baa7b9f35f66e5b3a9b8ad8123'],
  ['SKYPEDIA_EN_VI_388ADC0','Skypedia English-Vietnamese Dictionary','388adc0826300912e5b0311c089e0e70494b065f','https://github.com/skypediacode/english-vietnamese-dictionary','CC BY-SA 4.0','2b65bea9577c2a4149caa90940c806f321d1c6cfac503ed8bbd9f93e43a0033a'],
] as const;
async function seedMetadata(client:PoolClient){
  const topics=JSON.parse(await readFile(new URL('../datasets/processed/m15_5/topics.json',import.meta.url),'utf8')) as Array<{code:string;name:string;description:string}>;
  for(const topic of topics)await client.query(`INSERT INTO topics(code,name,description,image_url) VALUES($1,$2,$3,NULL)
    ON CONFLICT(code) DO NOTHING`,[topic.code,topic.name,topic.description]);
  for(const topic of topics)await client.query(`INSERT INTO vocabulary_review_topics(code,name,description,source_payload) VALUES($1,$2,$3,$4::jsonb)
    ON CONFLICT(code) DO NOTHING`,[topic.code,topic.name,topic.description,JSON.stringify(topic)]);
  for(const source of sources)await client.query(`INSERT INTO vocabulary_sources(code,name,version,source_url,license,checksum_sha256)
    VALUES($1,$2,$3,$4,$5,$6) ON CONFLICT(code) DO NOTHING`,[...source]);
}
try{
  const database=(await pool.query<{name:string}>('SELECT current_database() name')).rows[0]!.name;
  if(database!==env.DB_NAME)throw new Error('Connected database mismatch');
  const current=(await pool.query<{cards:number}>(`SELECT count(*)::int cards FROM flashcards`)).rows[0]!.cards;
  if(![5000,10000,15000,20000].includes(current)||current>target)throw new Error(`Unexpected current card count ${current}`);
  const internalDuplicates=(await pool.query<{count:number}>(`SELECT count(*)::int count FROM (
    SELECT lower(value->>'headword'),lower(value->>'vietnamese') FROM jsonb_array_elements($1::jsonb) value GROUP BY 1,2 HAVING count(*)>1)d`,[JSON.stringify(prefix)])).rows[0]!.count;
  const invalid=prefix.filter(c=>!c.candidateId||!c.headword?.trim()||!c.vietnamese?.trim()||!c.partOfSpeech||
    !c.primaryTopicCode||!c.topicCodes?.includes(c.primaryTopicCode)||c.contentStatus!=='IMPORTED_UNVERIFIED'||c.quizEligible!==false).length;
  if(internalDuplicates||invalid)throw new Error(`Technical preflight failed: duplicates=${internalDuplicates}, invalid=${invalid}`);
  const already=(await pool.query<{count:number}>(`SELECT count(*)::int count FROM vocabulary_review_candidates
    WHERE candidate_id=ANY($1::text[]) AND status='PUBLISHED' AND published_flashcard_id IS NOT NULL`,[prefix.map(c=>c.candidateId)])).rows[0]!.count;
  const expectedCurrent=5000+already;
  if(current!==expectedCurrent)throw new Error(`Database/plan state mismatch: cards=${current}, expected=${expectedCurrent}`);
  const summary={mode:apply?'apply':'dry-run',database,current,target,planSha256,selectedPrefix:prefix.length,
    alreadyImported:already,toInsert:prefix.length-already,internalDuplicates,invalid};
  if(!apply){console.log(JSON.stringify(summary,null,2));process.exit(0);}
  let inserted=0;
  for(let offset=already;offset<prefix.length;offset+=100){
    const batch=prefix.slice(offset,Math.min(prefix.length,offset+100));const client=await pool.connect();
    try{
      await client.query('BEGIN');await seedMetadata(client);
      const dictionarySource=(await client.query<{id:number}>(`SELECT id FROM vocabulary_sources WHERE code='SKYPEDIA_EN_VI_388ADC0'`)).rows[0]!.id;
      const cefrSource=(await client.query<{id:number}>(`SELECT id FROM vocabulary_sources WHERE code='WORDS_CEFR_2024'`)).rows[0]!.id;
      for(const candidate of batch){
        const existing=(await client.query<{published_flashcard_id:number}>(`SELECT published_flashcard_id FROM vocabulary_review_candidates
          WHERE candidate_id=$1 AND status='PUBLISHED'`,[candidate.candidateId])).rows[0];
        if(existing)continue;
        const duplicate=(await client.query(`SELECT id FROM flashcards WHERE lower(english)=lower($1::text COLLATE dataset_unicode)
          AND lower(vietnamese)=lower($2::text COLLATE dataset_unicode)`,[candidate.headword,candidate.vietnamese])).rows[0];
        if(duplicate)throw new Error(`Untracked duplicate ${candidate.candidateId}`);
        const primary=(await client.query<{id:number}>('SELECT id FROM topics WHERE code=$1',[candidate.primaryTopicCode])).rows[0];
        if(!primary)throw new Error(`Missing topic ${candidate.primaryTopicCode}`);
        const card=(await client.query<{id:number}>(`INSERT INTO flashcards(topic_id,english,vietnamese,pronunciation,part_of_speech,example,example_vi,level,is_active)
          VALUES($1,$2,$3,$4,$5,$6,$7,$8,TRUE) RETURNING id`,[primary.id,candidate.headword,candidate.vietnamese,candidate.pronunciation,
          candidate.partOfSpeech,candidate.example,candidate.exampleVi,candidate.cefr])).rows[0]!;
        for(const code of candidate.topicCodes)await client.query(`INSERT INTO flashcard_topics(flashcard_id,topic_id,is_primary)
          SELECT $1,id,code=$3 FROM topics WHERE code=$2`,[card.id,code,candidate.primaryTopicCode]);
        const reasons=JSON.stringify([...new Set([...candidate.validationReasons,'IMPORTED_UNVERIFIED','SEMANTIC_REVIEW_PENDING'])]);
        await client.query(`INSERT INTO flashcard_provenance(flashcard_id,source_id,source_record_id,source_word_id,source_definition_id,
          cefr_scope,validation_status,validation_reasons,quiz_eligible) VALUES($1,$2,$3,$4,$5,$6,'NEEDS_REVIEW',$7::jsonb,FALSE)`,
        [card.id,dictionarySource,candidate.candidateId,candidate.source.wordId??null,candidate.source.definitionId??null,candidate.cefrScope,reasons]);
        if(candidate.cefr)await client.query(`INSERT INTO flashcard_provenance(flashcard_id,source_id,source_record_id,cefr_scope,
          validation_status,validation_reasons,quiz_eligible) VALUES($1,$2,$3,$4,'NEEDS_REVIEW',$5::jsonb,FALSE)`,
        [card.id,cefrSource,`${candidate.candidateId}:cefr`,candidate.cefrScope,reasons]);
        const payload={...candidate,contentStatus:'IMPORTED_UNVERIFIED',semanticVerified:false};
        await client.query(`INSERT INTO vocabulary_review_candidates(candidate_id,english,vietnamese,pronunciation,part_of_speech,cefr,
          primary_topic_code,topic_codes,example,example_vi,source_code,source_name,source_url,source_version,source_license,source_record_id,
          source_word_id,source_definition_id,cefr_scope,validation_reasons,quality_score,quality_verified,license_status,status,published_flashcard_id,original_payload)
          VALUES($1::varchar,$2,$3,$4,$5,$6,$7,$8,$9,$10,'SKYPEDIA_EN_VI_388ADC0',$11,$12,$13,$14,$1::text,$15,$16,$17,$18,$19,FALSE,'ATTRIBUTION_REQUIRED','PUBLISHED',$20,$21::jsonb)
          ON CONFLICT(candidate_id) DO UPDATE SET status='PUBLISHED',published_flashcard_id=EXCLUDED.published_flashcard_id,
            quality_verified=FALSE,validation_reasons=EXCLUDED.validation_reasons,original_payload=EXCLUDED.original_payload,updated_at=CURRENT_TIMESTAMP
          WHERE vocabulary_review_candidates.status<>'PUBLISHED'`,[candidate.candidateId,candidate.headword,candidate.vietnamese,candidate.pronunciation,
          candidate.partOfSpeech,candidate.cefr,candidate.primaryTopicCode,candidate.topicCodes,candidate.example,candidate.exampleVi,candidate.source.name,
          candidate.source.url,candidate.source.version,candidate.source.license,candidate.source.wordId??null,candidate.source.definitionId??null,
          candidate.cefrScope,[...new Set([...candidate.validationReasons,'IMPORTED_UNVERIFIED','SEMANTIC_REVIEW_PENDING'])],candidate.qualityScore??0,card.id,JSON.stringify(payload)]);
        inserted++;
      }
      await client.query('COMMIT');
    }catch(error){await client.query('ROLLBACK');throw error;}finally{client.release();}
    const count=(await pool.query<{count:number}>('SELECT count(*)::int count FROM flashcards')).rows[0]!.count;
    console.log(JSON.stringify({checkpoint:target,batch:Math.floor(offset/100)+1,cards:count,inserted}));
  }
  const final=(await pool.query<{count:number}>('SELECT count(*)::int count FROM flashcards')).rows[0]!.count;
  if(final!==target)throw new Error(`Final count ${final} does not match target ${target}`);
  console.log(JSON.stringify({...summary,inserted,final,status:'DATA_IMPORT_DONE'},null,2));
}finally{await pool.end();}
