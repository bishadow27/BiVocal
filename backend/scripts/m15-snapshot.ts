import 'dotenv/config';import pg from 'pg';import {readDatabaseEnv} from '../src/config/env.js';
const e=readDatabaseEnv(process.env);const p=new pg.Pool({host:e.DB_HOST,port:e.DB_PORT,database:e.DB_NAME,user:e.DB_USER,password:e.DB_PASSWORD});
try{const base=(await p.query(`SELECT (SELECT count(*)::int FROM flashcards) flashcards,(SELECT count(*)::int FROM topics) topics,
 (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,(SELECT count(*)::int FROM quiz_results) quiz_results,
 (SELECT count(*)::int FROM user_scores) user_scores,(SELECT count(*)::int FROM xp_history) xp_history`)).rows[0];
 const exists=(await p.query(`SELECT to_regclass('public.user_learning_settings') IS NOT NULL settings,
 to_regclass('public.user_learned_flashcards') IS NOT NULL learned,to_regclass('public.user_learning_daily') IS NOT NULL daily,
 to_regclass('public.learning_system_config') IS NOT NULL config`)).rows[0];
 const m15=exists.settings?(await p.query(`SELECT (SELECT count(*)::int FROM user_learning_settings) settings,
 (SELECT count(*)::int FROM user_learned_flashcards) learned,(SELECT count(*)::int FROM user_learned_flashcards WHERE first_source='LEGACY') legacy,
 (SELECT count(*)::int FROM user_learning_daily) daily,(SELECT count(*)::int FROM learning_system_config) config`)).rows[0]:null;console.log(JSON.stringify({database:e.DB_NAME,base,m15Schema:exists,m15}));
}finally{await p.end();}
