import { AppError } from '../../shared/errors/app-error.js';

/** Read-only prerequisite check. Never creates or migrates the normal database. */
export async function assertQuizSessionSchema(db:{query(sql:string):Promise<{rows:{ready:boolean}[]}>}):Promise<void>{
 const result=await db.query(`SELECT
  to_regclass('public.quiz_sessions_v2') IS NOT NULL AND
  to_regclass('public.quiz_questions_v2') IS NOT NULL AND
  to_regclass('public.quiz_actions_v2') IS NOT NULL AND
  (SELECT count(*)=2 FROM information_schema.columns WHERE table_schema='public'
   AND table_name='xp_history' AND column_name IN ('quiz_action_id','quiz_action_operation')) AND
  NOT EXISTS (SELECT 1 FROM (VALUES
    ('quiz_v2_session_transition','quiz_sessions_v2'),
    ('quiz_v2_creation_snapshot','quiz_sessions_v2'),
    ('quiz_v2_question_snapshot','quiz_questions_v2'),
    ('quiz_v2_action_binding','quiz_actions_v2'),
    ('quiz_v2_final_receipt','quiz_actions_v2'),
    ('quiz_v2_xp_binding','xp_history')) AS required(name,relation)
    WHERE NOT EXISTS (SELECT 1 FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
      JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
      AND t.tgname=required.name AND c.relname=required.relation AND t.tgenabled IN ('O','A'))) AND
  (SELECT count(*)=2 FROM pg_constraint WHERE conrelid=to_regclass('public.xp_history')
    AND conname IN ('xp_history_v2_link','xp_history_v2_owner_answer_fk')) AND
  NOT EXISTS (SELECT 1 FROM (VALUES ('quiz_actions_v2_one_answer','quiz_actions_v2'),
    ('xp_history_quiz_action_unique','xp_history')) AS required(name,relation)
    WHERE NOT EXISTS (SELECT 1 FROM pg_index i JOIN pg_class idx ON idx.oid=i.indexrelid
      WHERE idx.oid=to_regclass('public.'||required.name)
      AND i.indrelid=to_regclass('public.'||required.relation)
      AND i.indisunique AND i.indisvalid AND i.indisready)) AS ready`);
 if(result.rows[0]?.ready!==true)throw new AppError(503,'QUIZ_V2_SCHEMA_REQUIRED','Quiz v2 requires an approved database schema upgrade.');
}
