import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const REQUIRED='bivoca_m15_5_20261024';
const env=readDatabaseEnv(process.env);
if(env.DB_NAME!==REQUIRED)throw new Error(`Verification requires DB_NAME=${REQUIRED}`);
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:2});
const scalar=async(sql:string,params:unknown[]=[])=>(await pool.query(sql,params)).rows[0];

try{
  const counts=await scalar(`SELECT (SELECT count(*)::int FROM flashcards) flashcards,
    (SELECT count(DISTINCT lower(trim(english)))::int FROM flashcards) distinct_english,
    (SELECT count(*)::int FROM topics) topics,(SELECT count(*)::int FROM users) users,
    (SELECT count(*)::int FROM user_progress) progress,(SELECT count(*)::int FROM quiz_results) quiz,
    (SELECT count(*)::int FROM user_scores) scores,(SELECT count(*)::int FROM xp_history) xp`);
  const quality=await scalar(`SELECT
    count(*) FILTER(WHERE trim(english)='' OR trim(vietnamese)='' OR part_of_speech IS NULL OR trim(part_of_speech)='')::int missing_required,
    count(*) FILTER(WHERE level IS NULL)::int cefr_undetermined,
    count(*) FILTER(WHERE level IS NOT NULL)::int cefr_known,
    (SELECT count(*)::int FROM (SELECT lower(english),lower(vietnamese) FROM flashcards GROUP BY 1,2 HAVING count(*)>1)d) duplicate_groups
    FROM flashcards`);
  const quiz=await scalar(`SELECT count(*)::int eligible FROM flashcards f WHERE f.is_active=TRUE AND
    (NOT EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id)
     OR EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id
       AND p.validation_status IN('APPROVED_AUTO','APPROVED_MANUAL') AND p.quiz_eligible=TRUE))`);
  const provenance=await scalar(`SELECT count(*) FILTER(WHERE f.level IS NULL)::int unknown_cefr_cards,
    count(*) FILTER(WHERE f.level IS NULL AND s.code='WORDS_CEFR_2024')::int false_cefr_provenance
    FROM flashcards f JOIN flashcard_provenance p ON p.flashcard_id=f.id JOIN vocabulary_sources s ON s.id=p.source_id`);
  const protectedData=await scalar(`SELECT
    md5(coalesce(string_agg(id::text||'|'||topic_id||'|'||english||'|'||vietnamese||'|'||coalesce(part_of_speech,'')||'|'||coalesce(level,''),'#' ORDER BY id),'')) cards_first_5000
    FROM flashcards WHERE id<=5000`);
  const userChecks=await scalar(`SELECT
    (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM users ORDER BY id)x) users,
    (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM user_progress ORDER BY id)x) progress,
    (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM quiz_results ORDER BY id)x) quiz,
    (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY user_id),'')) FROM (SELECT * FROM user_scores ORDER BY user_id)x) scores,
    (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM xp_history ORDER BY id)x) xp`);
  const status=(await pool.query(`SELECT status,count(*)::int count FROM vocabulary_review_candidates GROUP BY status ORDER BY status`)).rows;
  const levels=(await pool.query(`SELECT coalesce(level,'UNDETERMINED') level,count(*)::int count FROM flashcards GROUP BY level ORDER BY level NULLS LAST`)).rows;
  const topicCounts=(await pool.query(`SELECT t.code,count(f.id)::int cards FROM topics t LEFT JOIN flashcards f ON f.topic_id=t.id
    GROUP BY t.id ORDER BY cards DESC,t.code`)).rows;
  const plans=[] as Array<{name:string;planningMs:number;executionMs:number}>;
  for(const [name,sql,params] of [
    ['search',`SELECT f.id FROM flashcards f WHERE f.is_active=TRUE AND (f.english ILIKE $1 OR f.vietnamese ILIKE $1) ORDER BY lower(f.english),f.id LIMIT 20 OFFSET 0`,['%app%']],
    ['topic_page',`SELECT f.id FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE f.is_active=TRUE AND t.code=$1 ORDER BY lower(f.english),f.id LIMIT 20`,['GENERAL_VOCABULARY']],
    ['cefr_page',`SELECT f.id FROM flashcards f WHERE f.is_active=TRUE AND f.level=$1 ORDER BY lower(f.english),f.id LIMIT 20`,['B1']],
    ['quiz_pool',`SELECT f.id FROM flashcards f WHERE f.is_active=TRUE AND EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id AND p.validation_status IN('APPROVED_AUTO','APPROVED_MANUAL') AND p.quiz_eligible=TRUE) ORDER BY f.id LIMIT 40`,[]],
    ['admin_pending',`SELECT candidate_id FROM vocabulary_review_candidates WHERE status='PENDING' ORDER BY candidate_id LIMIT 100 OFFSET 1000`,[]],
  ] as Array<[string,string,unknown[]]>){
    const raw=(await pool.query(`EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON) ${sql}`,params)).rows[0]['QUERY PLAN'][0];
    plans.push({name,planningMs:Number(raw['Planning Time']),executionMs:Number(raw['Execution Time'])});
  }
  console.log(JSON.stringify({database:env.DB_NAME,counts,quality,quizEligible:quiz.eligible,provenance,
    protectedData,userChecks,status,levels,topicCounts,performance:plans},null,2));
}finally{await pool.end();}
