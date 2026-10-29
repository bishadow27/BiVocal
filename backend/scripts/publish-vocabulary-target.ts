import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';
import { createVocabularyReviewRepository } from '../src/modules/vocabulary-review/vocabulary-review.repository.js';
import { createVocabularyReviewService } from '../src/modules/vocabulary-review/vocabulary-review.service.js';

const args=process.argv.slice(2),apply=args.includes('--apply');
const value=(name:string)=>{const i=args.indexOf(name);return i>=0?args[i+1]:undefined;};
const target=Number(value('--target-total')??'5000'),batchSize=Number(value('--batch-size')??'100');
const env=readDatabaseEnv(process.env),confirmation=value('--database-confirm');
if(!Number.isSafeInteger(target)||target<358||!Number.isSafeInteger(batchSize)||batchSize<1||batchSize>100)
  throw new Error('Invalid target or batch size');
if(apply&&(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)||confirmation!==env.DB_NAME))
  throw new Error('Apply requires local PostgreSQL and exact --database-confirm');
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:4,connectionTimeoutMillis:5000});

try{
  const current=(await pool.query<{cards:number}>(`SELECT count(*)::int cards FROM flashcards`)).rows[0]!.cards;
  const need=Math.max(0,target-current);
  const actor=(await pool.query<{id:number}>(`SELECT u.id FROM users u WHERE u.role='ADMIN'
    ORDER BY (SELECT count(*) FROM vocabulary_review_candidates c WHERE c.reviewed_by=u.id AND c.status='PUBLISHED') DESC,u.id LIMIT 1`)).rows[0];
  if(!actor)throw new Error('No ADMIN account is available');
  const approved=(await pool.query<{candidate_id:string}>(`SELECT candidate_id FROM vocabulary_review_candidates
    WHERE status='APPROVED' AND quality_verified=TRUE
      AND (original_payload->'deploymentSelection'->>'targetTotal')::int=$2
      AND COALESCE((original_payload->'source'->>'semanticVerified')::boolean,FALSE)=TRUE
    ORDER BY candidate_id LIMIT $1`,[need,target])).rows.map(r=>r.candidate_id);
  if(approved.length!==need)throw new Error(`Expected ${need} approved candidates, found ${approved.length}`);
  const duplicateCheck=(await pool.query<{duplicates:number}>(`SELECT count(*)::int duplicates FROM vocabulary_review_candidates c
    JOIN flashcards f ON lower(f.english)=lower(c.english) AND lower(f.vietnamese)=lower(c.vietnamese)
    WHERE c.candidate_id=ANY($1::text[])`,[approved])).rows[0]!.duplicates;
  const plan={mode:apply?'apply':'dry-run',database:env.DB_NAME,currentCards:current,target,needed:need,
    approved:approved.length,exactDuplicates:duplicateCheck,batchSize,batches:Math.ceil(approved.length/batchSize)};
  if(duplicateCheck!==0)throw new Error(`Dry-run found ${duplicateCheck} exact duplicates`);
  if(!apply){console.log(JSON.stringify(plan,null,2));process.exit(0);}
  if(need===0){console.log(JSON.stringify({...plan,inserted:0},null,2));process.exit(0);}

  const service=createVocabularyReviewService(createVocabularyReviewRepository(pool));
  const previews:Array<{batchId:string;ids:string[];toInsert:number;toSkip:number;blocked:number}>=[];
  for(let offset=0;offset<approved.length;offset+=batchSize){
    const ids=approved.slice(offset,offset+batchSize),preview=await service.preview(actor.id,ids);
    if(preview.toInsert!==ids.length||preview.toSkip!==0||preview.blocked!==0)
      throw new Error(`Batch preview mismatch at offset ${offset}: ${preview.toInsert}/${preview.toSkip}/${preview.blocked}`);
    previews.push({batchId:preview.batchId,ids,toInsert:preview.toInsert,toSkip:preview.toSkip,blocked:preview.blocked});
  }
  console.log(JSON.stringify({...plan,previewed:previews.length},null,2));
  let inserted=0;
  for(let index=0;index<previews.length;index++){
    const result=await service.publish(actor.id,previews[index]!.batchId);
    inserted+=result.inserted;
    const cards=(await pool.query<{count:number}>('SELECT count(*)::int count FROM flashcards')).rows[0]!.count;
    const expected=current+inserted;
    if(cards!==expected)throw new Error(`Card count mismatch after batch ${index+1}: ${cards} != ${expected}`);
    console.log(JSON.stringify({batch:index+1,totalBatches:previews.length,inserted:result.inserted,totalInserted:inserted,cards}));
  }
  const final=(await pool.query<{count:number}>('SELECT count(*)::int count FROM flashcards')).rows[0]!.count;
  if(final!==target)throw new Error(`Final target mismatch: ${final} != ${target}`);
  console.log(JSON.stringify({...plan,inserted,finalCards:final,status:'COMPLETED'},null,2));
}finally{await pool.end();}
