import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const env=readDatabaseEnv(process.env);
if(env.DB_NAME!=='flashcard_english')throw new Error('Production verification requires DB_NAME=flashcard_english');
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:1});
try{
  const counts=(await pool.query(`SELECT
    (SELECT count(*)::int FROM flashcards) flashcards,
    (SELECT count(*)::int FROM topics) topics,
    (SELECT count(*)::int FROM users) users,
    (SELECT count(*)::int FROM user_progress) progress,
    (SELECT count(*)::int FROM quiz_results) quiz_results,
    (SELECT count(*)::int FROM user_scores) scores,
    (SELECT count(*)::int FROM xp_history) xp`)).rows[0];
  const integrity=(await pool.query(`SELECT
    count(*) FILTER(WHERE trim(english)='' OR trim(vietnamese)='' OR part_of_speech IS NULL OR trim(part_of_speech)='')::int missing_required,
    (SELECT count(*)::int FROM (SELECT lower(english),lower(vietnamese) FROM flashcards GROUP BY 1,2 HAVING count(*)>1)d) duplicate_groups
    FROM flashcards`)).rows[0];
  const importState=(await pool.query(`SELECT
    count(DISTINCT c.published_flashcard_id)::int imported_unverified,
    count(DISTINCT c.published_flashcard_id) FILTER(WHERE c.quality_verified)::int falsely_verified,
    count(DISTINCT c.published_flashcard_id) FILTER(WHERE NOT ('IMPORTED_UNVERIFIED'=ANY(c.validation_reasons)))::int missing_status_reason
    FROM vocabulary_review_candidates c WHERE c.status='PUBLISHED'
      AND c.original_payload->>'contentStatus'='IMPORTED_UNVERIFIED'`)).rows[0];
  const provenance=(await pool.query(`SELECT
    count(DISTINCT p.flashcard_id)::int imported_cards,
    count(*) FILTER(WHERE p.validation_status<>'NEEDS_REVIEW')::int incorrectly_approved_rows,
    count(*) FILTER(WHERE p.quiz_eligible)::int incorrectly_quiz_eligible_rows
    FROM flashcard_provenance p WHERE p.validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb`)).rows[0];
  const quizLeak=(await pool.query(`SELECT count(*)::int count FROM flashcards f
    WHERE EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id
      AND p.validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb)
    AND (NOT EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id)
      OR EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id
        AND p.validation_status IN('APPROVED_AUTO','APPROVED_MANUAL') AND p.quiz_eligible=TRUE))`)).rows[0].count;
  const pagination=(await pool.query(`SELECT count(*)::int returned,min(id)::int min_id,max(id)::int max_id FROM (
    SELECT id FROM flashcards WHERE is_active=TRUE ORDER BY lower(english),id LIMIT 20 OFFSET 10000)s`)).rows[0];
  const general=(await pool.query(`SELECT count(*)::int cards FROM flashcards f JOIN topics t ON t.id=f.topic_id
    WHERE t.code='GENERAL_VOCABULARY'`)).rows[0].cards;
  const pass=counts.flashcards===20000&&counts.topics===20&&integrity.missing_required===0&&integrity.duplicate_groups===0&&
    importState.imported_unverified===15000&&importState.falsely_verified===0&&importState.missing_status_reason===0&&
    provenance.imported_cards===15000&&provenance.incorrectly_approved_rows===0&&provenance.incorrectly_quiz_eligible_rows===0&&quizLeak===0&&pagination.returned===20;
  const result={database:env.DB_NAME,counts,integrity,importState,provenance,quizLeak,pagination,generalVocabulary:general,pass};
  console.log(JSON.stringify(result,null,2));if(!pass)process.exitCode=1;
}finally{await pool.end();}
