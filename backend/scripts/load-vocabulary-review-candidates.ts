import 'dotenv/config';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import pg from 'pg';

const args=process.argv.slice(2),apply=args.includes('--apply'),confirmAt=args.indexOf('--database-confirm');
const value=(name:string)=>{const i=args.indexOf(name);return i>=0?args[i+1]:undefined;};
const inputDir=value('--input-dir')??'processed';
if(args.some((v,i)=>!['--apply','--dry-run','--database-confirm','--input-dir'].includes(v)&&
  i!==confirmAt+1&&i!==args.indexOf('--input-dir')+1))
  throw new Error('Usage: tsx scripts/load-vocabulary-review-candidates.ts [--input-dir DIR] [--dry-run|--apply --database-confirm DB_NAME]');
for(const key of ['DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD'] as const)if(!process.env[key])throw new Error(`Missing ${key}`);
const database=process.env.DB_NAME!,host=process.env.DB_HOST!;
if(apply&&(!['localhost','127.0.0.1','::1'].includes(host)||confirmAt<0||args[confirmAt+1]!==database))
  throw new Error('Review candidate load requires local PostgreSQL and an exact --database-confirm');
const datasetRoot=fileURLToPath(new URL('../datasets/',import.meta.url));
const inputUrl=path.join(datasetRoot,inputDir,inputDir==='processed'?'bivoca_vocab_candidates.jsonl':'candidates.jsonl');
const topicsUrl=path.join(datasetRoot,inputDir,inputDir==='processed'?'bivoca_topics.json':'topics.json');
const text=await readFile(inputUrl,'utf8'),lines=text.trim().split(/\r?\n/).filter(Boolean),topics=JSON.parse(await readFile(topicsUrl,'utf8'));
const sha256=createHash('sha256').update(text).digest('hex');
if(!apply){console.log(JSON.stringify({mode:'dry-run',database,candidates:lines.length,topics:topics.length,sha256},null,2));process.exit(0);}
const pool=new pg.Pool({host,port:Number(process.env.DB_PORT),database,user:process.env.DB_USER,password:process.env.DB_PASSWORD,max:2});
const client=await pool.connect();let inserted=0,skipped=0;
try{
  const current=(await client.query<{name:string}>('SELECT current_database() name')).rows[0]?.name;if(current!==database)throw new Error('Connected database mismatch');
  const ready=(await client.query<{ok:boolean}>(`SELECT to_regclass('public.vocabulary_review_candidates') IS NOT NULL
    AND to_regclass('public.flashcard_provenance') IS NOT NULL AS ok`)).rows[0]?.ok;if(!ready)throw new Error('M13.6 migrations are not applied');
  await client.query('BEGIN');
  for(const t of topics)await client.query(`INSERT INTO vocabulary_review_topics(code,name,description,source_payload)
    VALUES($1,$2,$3,$4::jsonb) ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,source_payload=EXCLUDED.source_payload`,
  [t.code,t.name,t.description,JSON.stringify(t)]);
  const sources=[
    ['WORDS_CEFR_2024','Words CEFR Dataset','local-sha256:c5e38190','https://github.com/Maximax67/Words-CEFR-Dataset','MIT','c5e381905a87b47bd1321ac1cd34f870b107c0baa7b9f35f66e5b3a9b8ad8123'],
    ['SKYPEDIA_EN_VI_388ADC0','Skypedia English-Vietnamese Dictionary','388adc0826300912e5b0311c089e0e70494b065f','https://github.com/skypediacode/english-vietnamese-dictionary','CC BY-SA 4.0','2b65bea9577c2a4149caa90940c806f321d1c6cfac503ed8bbd9f93e43a0033a'],
  ];
  for(const s of sources)await client.query(`INSERT INTO vocabulary_sources(code,name,version,source_url,license,checksum_sha256)
    VALUES($1,$2,$3,$4,$5,$6) ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,version=EXCLUDED.version,
    source_url=EXCLUDED.source_url,license=EXCLUDED.license,checksum_sha256=EXCLUDED.checksum_sha256`,s);
  for(let offset=0;offset<lines.length;offset+=400){
    const values=lines.slice(offset,offset+400).map(line=>{
      const value=JSON.parse(line);
      // PostgreSQL jsonb cannot represent U+0000; this normalized lookup key is
      // reproducible from the reviewed fields and is not part of the audit payload.
      delete value.normalizedKey;
      return value;
    });
    const result=await client.query(`WITH input AS (SELECT value FROM jsonb_array_elements($1::jsonb) value), saved AS (
      INSERT INTO vocabulary_review_candidates(candidate_id,english,vietnamese,pronunciation,part_of_speech,cefr,
        primary_topic_code,topic_codes,example,example_vi,source_code,source_name,source_url,source_version,source_license,
        source_record_id,source_word_id,source_definition_id,cefr_scope,validation_reasons,quality_score,quality_verified,
        license_status,status,original_payload)
      SELECT value->>'candidateId',value->>'headword',value->>'vietnamese',NULLIF(value->>'pronunciation',''),
        NULLIF(value->>'partOfSpeech',''),NULLIF(value->>'cefr',''),NULLIF(value->>'primaryTopicCode',''),
        ARRAY(SELECT jsonb_array_elements_text(COALESCE(value->'topicCodes','[]'::jsonb))),NULLIF(value->>'example',''),
        NULLIF(value->>'exampleVi',''),'SKYPEDIA_EN_VI_388ADC0',value->'source'->>'name',value->'source'->>'url',
        value->'source'->>'version',value->'source'->>'license',value->>'candidateId',(value->'source'->>'wordId')::bigint,
        (value->'source'->>'definitionId')::bigint,value->>'cefrScope',
        ARRAY(SELECT jsonb_array_elements_text(COALESCE(value->'validationReasons','[]'::jsonb))),
        COALESCE((value->>'qualityScore')::int,0),(value->>'validationStatus')='APPROVED_AUTO','ATTRIBUTION_REQUIRED',
        CASE value->>'validationStatus' WHEN 'APPROVED_AUTO' THEN 'APPROVED' WHEN 'REJECTED' THEN 'REJECTED' ELSE 'PENDING' END,value
      FROM input ON CONFLICT(candidate_id) DO NOTHING RETURNING 1)
      SELECT count(*)::int inserted FROM saved`,[JSON.stringify(values)]);
    const n=result.rows[0].inserted as number;inserted+=n;skipped+=values.length-n;
  }
  await client.query('COMMIT');console.log(JSON.stringify({mode:'apply',database,total:lines.length,inserted,skipped,sha256},null,2));
}catch(e){await client.query('ROLLBACK');const msg=e instanceof Error?e.message:'Review candidate load failed';console.error(process.env.DB_PASSWORD?msg.replaceAll(process.env.DB_PASSWORD,'[REDACTED]'):msg);process.exitCode=1;}
finally{client.release();await pool.end();}

