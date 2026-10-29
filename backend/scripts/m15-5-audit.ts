import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const env = readDatabaseEnv(process.env);
const pool = new pg.Pool({
  host: env.DB_HOST, port: env.DB_PORT, database: env.DB_NAME,
  user: env.DB_USER, password: env.DB_PASSWORD, max: 2,
});

try {
  const database = (await pool.query<{ name: string }>('SELECT current_database() name')).rows[0]!.name;
  const counts = (await pool.query(`SELECT
    (SELECT count(*)::int FROM flashcards) flashcards,
    (SELECT count(DISTINCT lower(trim(english)))::int FROM flashcards) distinct_english,
    (SELECT count(DISTINCT (lower(trim(english)),coalesce(lower(trim(part_of_speech)),''),lower(trim(vietnamese))))::int FROM flashcards) distinct_senses,
    (SELECT count(*)::int FROM topics) topics,
    (SELECT count(*)::int FROM users) users,
    (SELECT count(*)::int FROM user_progress) progress,
    (SELECT count(*)::int FROM quiz_results) quiz_results,
    (SELECT count(*)::int FROM user_scores) user_scores,
    (SELECT count(*)::int FROM xp_history) xp_history`)).rows[0];
  const reviewStatuses = (await pool.query(`SELECT status,count(*)::int count
    FROM vocabulary_review_candidates GROUP BY status ORDER BY status`)).rows;
  const reviewReasons = (await pool.query(`SELECT reason,count(*)::int count FROM
    (SELECT unnest(validation_reasons) reason FROM vocabulary_review_candidates) x
    GROUP BY reason ORDER BY count DESC,reason`)).rows;
  const provenance = (await pool.query(`SELECT
    count(*)::int rows,
    count(DISTINCT flashcard_id)::int cards,
    count(*) FILTER(WHERE quiz_eligible)::int quiz_eligible,
    count(*) FILTER(WHERE validation_status='APPROVED_AUTO')::int approved_auto,
    count(*) FILTER(WHERE validation_status='APPROVED_MANUAL')::int approved_manual,
    count(*) FILTER(WHERE validation_status='NEEDS_REVIEW')::int needs_review,
    count(*) FILTER(WHERE validation_status='REJECTED')::int rejected
    FROM flashcard_provenance`)).rows[0];
  const quizEligible = (await pool.query(`SELECT count(*)::int count FROM flashcards f WHERE f.is_active=TRUE AND
    (NOT EXISTS (SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id)
     OR EXISTS (SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id
       AND p.validation_status IN ('APPROVED_AUTO','APPROVED_MANUAL') AND p.quiz_eligible=TRUE))`)).rows[0]!.count;
  const duplicates = (await pool.query(`SELECT
    (SELECT count(*)::int FROM (SELECT lower(trim(english)),coalesce(lower(trim(part_of_speech)),''),lower(trim(vietnamese))
      FROM flashcards GROUP BY 1,2,3 HAVING count(*)>1) d) exact_sense_groups,
    (SELECT count(*)::int FROM (SELECT topic_id,lower(trim(vietnamese)) FROM flashcards
      GROUP BY 1,2 HAVING count(*)>1) d) topic_answer_groups`)).rows[0];
  const cefr = (await pool.query(`SELECT coalesce(level,'UNDETERMINED') level,count(*)::int count
    FROM flashcards GROUP BY level ORDER BY level NULLS LAST`)).rows;
  const topics = (await pool.query(`SELECT t.code,t.name,count(f.id)::int cards
    FROM topics t LEFT JOIN flashcards f ON f.topic_id=t.id GROUP BY t.id ORDER BY cards DESC,t.code`)).rows;
  const indexes = (await pool.query(`SELECT tablename,indexname,indexdef FROM pg_indexes
    WHERE schemaname='public' AND tablename IN ('flashcards','flashcard_topics','vocabulary_review_candidates')
    ORDER BY tablename,indexname`)).rows;
  console.log(JSON.stringify({database,counts,reviewStatuses,reviewReasons,provenance,
    quizEligible,duplicates,cefr,topics,indexes},null,2));
} finally {
  await pool.end();
}
