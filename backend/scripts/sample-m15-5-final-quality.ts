import 'dotenv/config';
import fs from 'node:fs';
import path from 'node:path';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const REQUIRED_DB='bivoca_m15_5_20261024',SAMPLE_SIZE=500,SEED='M15.5-DATAFIX-QG-20261025';
const output=path.resolve('.m15-5-verification/quality-sample-500-datafix.json');
const csvOutput=path.resolve('.m15-5-verification/quality-sample-500-datafix.csv');
const env=readDatabaseEnv(process.env);
if(env.DB_NAME!==REQUIRED_DB)throw new Error(`Final quality sampling requires DB_NAME=${REQUIRED_DB}`);
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:1});
const csv=(value:unknown)=>`"${String(value??'').replaceAll('"','""')}"`;
try{
  const result=await pool.query(`WITH eligible AS (
    SELECT DISTINCT ON(f.id) f.id,t.code topic,f.english,f.vietnamese,f.pronunciation,f.part_of_speech,
      f.example,f.example_vi,f.level,p.validation_status,p.quiz_eligible,s.code source_code,s.license source_license,
      c.candidate_id,c.original_payload,
      coalesce((c.original_payload->'source'->>'independentLexicalOverlap')::boolean,false) independent_lexical_overlap,
      coalesce((c.original_payload->'source'->>'semanticVerified')::boolean,false) semantic_verified,
      row_number() OVER(PARTITION BY t.code ORDER BY md5(f.id::text||':TOPIC:'||$1)) topic_rank,
      row_number() OVER(PARTITION BY coalesce(f.part_of_speech,'UNKNOWN') ORDER BY md5(f.id::text||':POS:'||$1)) pos_rank,
      row_number() OVER(PARTITION BY coalesce(f.level,'UNDETERMINED') ORDER BY md5(f.id::text||':CEFR:'||$1)) cefr_rank,
      row_number() OVER(PARTITION BY (f.level IS NULL) ORDER BY md5(f.id::text||':KNOWN:'||$1)) known_rank
    FROM flashcards f JOIN topics t ON t.id=f.topic_id
    JOIN vocabulary_review_candidates c ON c.published_flashcard_id=f.id
    JOIN flashcard_provenance p ON p.flashcard_id=f.id
    JOIN vocabulary_sources s ON s.id=p.source_id
    WHERE c.original_payload->>'schemaVersion'='2' AND s.code LIKE 'SKYPEDIA_EN_VI_%'
    ORDER BY f.id,s.code
  ), strata AS (
    SELECT * FROM eligible WHERE topic_rank<=5 OR pos_rank<=12 OR cefr_rank<=20 OR known_rank<=40
  ), first_half AS (
    SELECT * FROM strata ORDER BY md5(id::text||':STRATA:'||$1) LIMIT 250
  ), random_half AS (
    SELECT e.* FROM eligible e WHERE NOT EXISTS(SELECT 1 FROM first_half h WHERE h.id=e.id)
    ORDER BY md5(e.id::text||':RANDOM:'||$1) LIMIT 250
  ) SELECT * FROM(SELECT * FROM first_half UNION ALL SELECT * FROM random_half) q
    ORDER BY md5(id::text||':OUTPUT:'||$1)`,[SEED]);
  if(result.rows.length!==SAMPLE_SIZE)throw new Error(`Expected ${SAMPLE_SIZE}, got ${result.rows.length}`);
  const rows=result.rows,groups=(key:string)=>Object.fromEntries([...new Set(rows.map(r=>String(r[key]??'UNDETERMINED')))]
    .sort().map(v=>[v,rows.filter(r=>String(r[key]??'UNDETERMINED')===v).length]));
  const summary={database:REQUIRED_DB,seed:SEED,population:'15,000 published M15.5 additions',sampleSize:rows.length,
    topics:groups('topic'),partsOfSpeech:groups('part_of_speech'),cefr:groups('level'),
    optionalFields:{pronunciationPresent:rows.filter(r=>r.pronunciation).length,examplePresent:rows.filter(r=>r.example).length},
    independentLexicalOverlap:rows.filter(r=>r.independent_lexical_overlap).length,
    semanticVerified:rows.filter(r=>r.semantic_verified).length,
    provenance:{source:'SKYPEDIA_EN_VI_388ADC0',license:'CC BY-SA 4.0',cefrOnlyWhenProvenanced:true}};
  fs.mkdirSync(path.dirname(output),{recursive:true});
  fs.writeFileSync(output,JSON.stringify({summary,rows},null,2)+'\n','utf8');
  const keys=['id','topic','part_of_speech','level','english','vietnamese','pronunciation','example','source_code','source_license','independent_lexical_overlap','semantic_verified'];
  fs.writeFileSync(csvOutput,[keys.map(csv).join(','),...rows.map(r=>keys.map(k=>csv(r[k])).join(','))].join('\n')+'\n','utf8');
  console.log(JSON.stringify(summary,null,2));
}finally{await pool.end();}
