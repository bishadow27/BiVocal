import 'dotenv/config';
import { readFile } from 'node:fs/promises';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

type Candidate = {
  candidateId:string; validationReasons:string[]; validationStatus:string;
  [key:string]:unknown;
};

const args=process.argv.slice(2),apply=args.includes('--apply');
const value=(name:string)=>{const index=args.indexOf(name);return index>=0?args[index+1]:undefined;};
const confirmation=value('--database-confirm');
const env=readDatabaseEnv(process.env);
if(apply&&(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)||confirmation!==env.DB_NAME))
  throw new Error('Apply requires local PostgreSQL and exact --database-confirm');

const source=await readFile(new URL('../datasets/processed/bivoca_vocab_candidates.jsonl',import.meta.url),'utf8');
const uncertain=(source.split(/\r?\n/).filter(Boolean).map(line=>JSON.parse(line) as Candidate)
  .filter(candidate=>candidate.validationReasons.includes('POSSIBLE_DUPLICATE_MEANING')));
const byId=new Map(uncertain.map(candidate=>[candidate.candidateId,candidate]));
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,
  user:env.DB_USER,password:env.DB_PASSWORD,max:1,connectionTimeoutMillis:5000});

try{
  const database=(await pool.query<{name:string}>('SELECT current_database() name')).rows[0]!.name;
  if(database!==env.DB_NAME)throw new Error('Connected database mismatch');
  const rows=(await pool.query<{candidate_id:string;published_flashcard_id:number}>(`
    SELECT candidate_id,published_flashcard_id FROM vocabulary_review_candidates
    WHERE candidate_id=ANY($1::text[]) AND status='PUBLISHED'
    ORDER BY candidate_id`,[[...byId.keys()]])).rows;
  const cardIds=rows.map(row=>row.published_flashcard_id);
  if(cardIds.some(id=>id<=364))throw new Error('Protected original flashcard selected');
  let references={progress:0,quiz:0,xp:0,batchDuplicates:0};
  if(cardIds.length){
    references=(await pool.query<typeof references>(`SELECT
      (SELECT count(*)::int FROM user_progress WHERE flashcard_id=ANY($1::int[])) progress,
      (SELECT count(*)::int FROM quiz_results WHERE flashcard_id=ANY($1::int[]) OR selected_flashcard_id=ANY($1::int[])) quiz,
      (SELECT count(*)::int FROM xp_history WHERE flashcard_id=ANY($1::int[])) xp,
      (SELECT count(*)::int FROM vocabulary_publish_batch_items WHERE duplicate_flashcard_id=ANY($1::int[])) "batchDuplicates"`,[cardIds])).rows[0]!;
  }
  const plan={mode:apply?'apply':'dry-run',database,detected:uncertain.length,
    publishedAffected:rows.length,cardIds,references};
  if(!apply){console.log(JSON.stringify(plan,null,2));process.exit(0);}
  if(Object.values(references).some(count=>count!==0))throw new Error('Affected cards already have dependent user or batch data');

  const actor=(await pool.query<{id:number}>(`SELECT id FROM users WHERE role='ADMIN' ORDER BY id LIMIT 1`)).rows[0];
  if(!actor)throw new Error('No ADMIN account is available for audit attribution');
  const client=await pool.connect();
  try{
    await client.query('BEGIN');
    for(const row of rows){
      const candidate=byId.get(row.candidate_id)!;
      const payload={...candidate};
      delete payload.normalizedKey;
      await client.query(`UPDATE vocabulary_review_candidates SET status='PENDING',quality_verified=FALSE,
        published_flashcard_id=NULL,validation_reasons=$2,original_payload=$3::jsonb,
        reviewed_by=$4,reviewed_at=CURRENT_TIMESTAMP,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP
        WHERE candidate_id=$1 AND status='PUBLISHED' AND published_flashcard_id=$5`,
      [row.candidate_id,candidate.validationReasons,JSON.stringify(payload),actor.id,row.published_flashcard_id]);
      await client.query(`INSERT INTO vocabulary_review_audit(candidate_id,actor_user_id,action,before_value,after_value)
        VALUES($1,$2,'BULK_RESET_PENDING',$3::jsonb,$4::jsonb)`,[row.candidate_id,actor.id,
        JSON.stringify({status:'PUBLISHED',flashcardId:row.published_flashcard_id,source:'M13_7_NEAR_DUPLICATE_FIX'}),
        JSON.stringify({status:'PENDING',reason:'POSSIBLE_DUPLICATE_MEANING',source:'M13_7_NEAR_DUPLICATE_FIX'})]);
    }
    if(cardIds.length)await client.query('DELETE FROM flashcards WHERE id=ANY($1::int[])',[cardIds]);
    await client.query('COMMIT');
  }catch(error){await client.query('ROLLBACK');throw error;}finally{client.release();}
  const remaining=(await pool.query<{count:number}>('SELECT count(*)::int count FROM flashcards')).rows[0]!.count;
  console.log(JSON.stringify({...plan,requeued:rows.length,deletedCards:cardIds.length,remainingCards:remaining},null,2));
}finally{await pool.end();}
