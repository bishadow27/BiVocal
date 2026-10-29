import 'dotenv/config';import pg from 'pg';import {readDatabaseEnv} from '../src/config/env.js';
const POS=['POS_NOUN','POS_VERB','POS_ADJECTIVE','POS_ADVERB','POS_PREPOSITION','POS_CONJUNCTION','POS_PRONOUN','POS_PHRASE'];
const args=process.argv.slice(2),confirm=args[args.indexOf('--database-confirm')+1],env=readDatabaseEnv(process.env);if(confirm!==env.DB_NAME)throw new Error('Exact database confirmation required');
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:1});
try{const counts=(await pool.query(`SELECT (SELECT count(*)::int FROM flashcards) flashcards,(SELECT count(*)::int FROM topics) topics,
 (SELECT count(*)::int FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.code=ANY($1::text[])) pos,
 (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,(SELECT count(*)::int FROM quiz_results) quiz,
 (SELECT count(*)::int FROM user_scores) scores,(SELECT count(*)::int FROM xp_history) xp`,[POS])).rows[0];
 const integrity=(await pool.query(`SELECT
 (SELECT count(*)::int FROM flashcard_topics WHERE is_primary=TRUE AND topic_id IS NULL) invalid_primary,
 (SELECT count(*)::int FROM flashcards f WHERE NOT EXISTS(SELECT 1 FROM flashcard_topics ft WHERE ft.flashcard_id=f.id AND ft.is_primary=TRUE)) missing_primary,
 (SELECT count(*)::int FROM flashcards f JOIN flashcard_topics ft ON ft.flashcard_id=f.id AND ft.is_primary=TRUE WHERE ft.topic_id<>f.topic_id) mismatched_primary,
 (SELECT count(*)::int FROM (SELECT flashcard_id FROM flashcard_topics WHERE is_primary=TRUE GROUP BY flashcard_id HAVING count(*)<>1)x) duplicate_primary,
 (SELECT count(*)::int FROM flashcard_provenance WHERE validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb AND quiz_eligible=TRUE) unverified_quiz_eligible,
 (SELECT count(DISTINCT flashcard_id)::int FROM flashcard_provenance WHERE validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb) imported_unverified`)).rows[0];
 const distribution=(await pool.query(`SELECT CASE WHEN t.code LIKE 'POS\\_%' ESCAPE '\\' THEN 'PART_OF_SPEECH' ELSE 'SEMANTIC' END topic_group,t.code,t.name,count(f.id)::int cards FROM topics t LEFT JOIN flashcards f ON f.topic_id=t.id GROUP BY t.id,t.code,t.name ORDER BY topic_group,t.code`)).rows;
 const pass=counts.flashcards===20000&&counts.topics===38&&counts.pos<12858&&Object.entries(integrity).every(([k,v])=>k==='imported_unverified'?v===15000:v===0);console.log(JSON.stringify({database:env.DB_NAME,counts,integrity,distribution,pass},null,2));if(!pass)process.exitCode=1;
}finally{await pool.end();}
