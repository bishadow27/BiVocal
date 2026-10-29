import 'dotenv/config';
import fs from 'node:fs';
import path from 'node:path';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const REQUIRED_DB = 'bivoca_m15_5_20261024';
const SAMPLE_SIZE = 240;
const OUTPUT = path.resolve('.m15-5-verification/quality-sample-240.json');
const env = readDatabaseEnv(process.env);
if (env.DB_NAME !== REQUIRED_DB) throw new Error(`Quality sampling requires DB_NAME=${REQUIRED_DB}`);
const pool = new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:1});

try {
  const result = await pool.query(`
    WITH eligible AS (
      SELECT f.id,t.code topic,f.english,f.vietnamese,f.pronunciation,f.part_of_speech,
             f.example,f.example_vi,f.level,p.validation_status,p.quiz_eligible,
             s.code source_code,s.name source_name,s.license source_license,
             row_number() OVER (PARTITION BY t.code ORDER BY md5(f.id::text || ':M15.5-QG-20261024')) topic_rank,
             row_number() OVER (PARTITION BY coalesce(f.part_of_speech,'UNKNOWN') ORDER BY md5(f.id::text || ':POS')) pos_rank,
             row_number() OVER (PARTITION BY coalesce(f.level,'UNDETERMINED') ORDER BY md5(f.id::text || ':CEFR')) cefr_rank
      FROM flashcards f
      JOIN topics t ON t.id=f.topic_id
      JOIN flashcard_provenance p ON p.flashcard_id=f.id
      JOIN vocabulary_sources s ON s.id=p.source_id
      JOIN vocabulary_review_candidates c ON c.published_flashcard_id=f.id
      WHERE c.original_payload->>'schemaVersion'='2' AND s.code LIKE 'SKYPEDIA_EN_VI_%'
    ), strata AS (
      SELECT * FROM eligible WHERE topic_rank <= 3 OR pos_rank <= 3 OR cefr_rank <= 5
    ), chosen AS (
      SELECT * FROM strata ORDER BY md5(id::text || ':STRATA') LIMIT 120
    ), random_fill AS (
      SELECT e.* FROM eligible e WHERE NOT EXISTS(SELECT 1 FROM chosen c WHERE c.id=e.id)
      ORDER BY md5(e.id::text || ':RANDOM') LIMIT $1
    )
    SELECT u.* FROM (SELECT * FROM chosen UNION ALL SELECT * FROM random_fill) u
    ORDER BY md5(u.id::text || ':OUTPUT') LIMIT $2`, [SAMPLE_SIZE, SAMPLE_SIZE]);
  const rows=result.rows;
  if(rows.length!==SAMPLE_SIZE) throw new Error(`Expected ${SAMPLE_SIZE} rows, got ${rows.length}`);
  const summary={
    database:REQUIRED_DB, seed:'M15.5-QG-20261024', population:'published M15.5 flashcards with schemaVersion=2',
    sampleSize:rows.length,
    topics:Object.fromEntries([...new Set(rows.map(r=>r.topic))].sort().map(k=>[k,rows.filter(r=>r.topic===k).length])),
    partsOfSpeech:Object.fromEntries([...new Set(rows.map(r=>r.part_of_speech??'UNKNOWN'))].sort().map(k=>[k,rows.filter(r=>(r.part_of_speech??'UNKNOWN')===k).length])),
    cefr:Object.fromEntries([...new Set(rows.map(r=>r.level??'UNDETERMINED'))].sort().map(k=>[k,rows.filter(r=>(r.level??'UNDETERMINED')===k).length])),
    sources:Object.fromEntries([...new Set(rows.map(r=>r.source_code))].sort().map(k=>[k,rows.filter(r=>r.source_code===k).length])),
  };
  fs.mkdirSync(path.dirname(OUTPUT),{recursive:true});
  fs.writeFileSync(OUTPUT,JSON.stringify({summary,rows},null,2)+'\n','utf8');
  console.log(JSON.stringify(summary,null,2));
} finally { await pool.end(); }
