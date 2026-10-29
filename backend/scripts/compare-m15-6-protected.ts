import 'dotenv/config';import pg from 'pg';import {readDatabaseEnv} from '../src/config/env.js';
const env=readDatabaseEnv(process.env);if(env.DB_NAME!=='flashcard_english')throw new Error('Requires production env');
const config={host:env.DB_HOST,port:env.DB_PORT,user:env.DB_USER,password:env.DB_PASSWORD};
const production=new pg.Pool({...config,database:'flashcard_english',max:1}),isolated=new pg.Pool({...config,database:'bivoca_m15_6_20261025',max:1});
const sql=`SELECT
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT id,english,vietnamese,pronunciation,part_of_speech,example,example_vi,level,is_active,created_at FROM flashcards ORDER BY id)x) card_content,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM users ORDER BY id)x) users,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM user_progress ORDER BY id)x) progress,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM quiz_results ORDER BY id)x) quiz,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY user_id),'')) FROM (SELECT * FROM user_scores ORDER BY user_id)x) scores,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM xp_history ORDER BY id)x) xp,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY flashcard_id,source_id),'')) FROM (SELECT * FROM flashcard_provenance ORDER BY flashcard_id,source_id)x) provenance`;
try{const a=(await production.query(sql)).rows[0],b=(await isolated.query(sql)).rows[0],matches=Object.fromEntries(Object.keys(a).map(key=>[key,a[key]===b[key]]));console.log(JSON.stringify({matches,allMatch:Object.values(matches).every(Boolean)},null,2));if(!Object.values(matches).every(Boolean))process.exitCode=1;}finally{await Promise.all([production.end(),isolated.end()]);}
