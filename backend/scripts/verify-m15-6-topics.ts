import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';
const env=readDatabaseEnv(process.env),pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:1});
try{
 const counts=(await pool.query(`SELECT (SELECT count(*)::int FROM flashcards) flashcards,(SELECT count(*)::int FROM topics) topics,
  (SELECT count(*)::int FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.code='GENERAL_VOCABULARY') general_primary,
  (SELECT count(*)::int FROM flashcard_topics ft JOIN topics t ON t.id=ft.topic_id WHERE t.code='GENERAL_VOCABULARY') general_links,
  (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,(SELECT count(*)::int FROM quiz_results) quiz,
  (SELECT count(*)::int FROM user_scores) scores,(SELECT count(*)::int FROM xp_history) xp`)).rows[0];
 const integrity=(await pool.query(`SELECT
  (SELECT count(*)::int FROM (SELECT flashcard_id FROM flashcard_topics WHERE is_primary GROUP BY flashcard_id HAVING count(*)<>1)x) invalid_primary,
  (SELECT count(*)::int FROM flashcards f LEFT JOIN flashcard_topics ft ON ft.flashcard_id=f.id AND ft.is_primary WHERE ft.flashcard_id IS NULL) missing_primary,
  (SELECT count(*)::int FROM flashcards f JOIN flashcard_topics ft ON ft.flashcard_id=f.id AND ft.is_primary WHERE f.topic_id<>ft.topic_id) mismatched_primary,
  (SELECT count(*)::int FROM flashcard_provenance WHERE validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb AND quiz_eligible) unverified_quiz_eligible,
  (SELECT count(DISTINCT flashcard_id)::int FROM flashcard_provenance WHERE validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb) imported_unverified`)).rows[0];
 const distribution=(await pool.query(`SELECT CASE WHEN t.code LIKE 'POS\\_%' ESCAPE '\\' THEN 'PART_OF_SPEECH' ELSE 'SEMANTIC' END topic_group,
  t.code,t.name,count(f.id)::int cards FROM topics t LEFT JOIN flashcards f ON f.topic_id=t.id AND f.is_active GROUP BY t.id ORDER BY topic_group,t.name`)).rows;
 const pass=counts.flashcards===20000&&counts.topics===38&&counts.general_primary===0&&counts.general_links===0&&integrity.invalid_primary===0&&integrity.missing_primary===0&&integrity.mismatched_primary===0&&integrity.unverified_quiz_eligible===0&&integrity.imported_unverified===15000;
 console.log(JSON.stringify({database:env.DB_NAME,counts,integrity,distribution,pass},null,2));if(!pass)process.exitCode=1;
}finally{await pool.end();}
