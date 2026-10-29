import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const env=readDatabaseEnv(process.env);if(env.DB_NAME!=='flashcard_english')throw new Error('Read-only comparison requires production DB_NAME');
const config={host:env.DB_HOST,port:env.DB_PORT,user:env.DB_USER,password:env.DB_PASSWORD};
const prod=new pg.Pool({...config,database:'flashcard_english',max:1});
const isolated=new pg.Pool({...config,database:'bivoca_m15_5_20261024',max:1});
const sql=`SELECT
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM flashcards WHERE id<=5000 ORDER BY id)x) cards,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM users ORDER BY id)x) users,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM user_progress ORDER BY id)x) progress,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM quiz_results ORDER BY id)x) quiz,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY user_id),'')) FROM (SELECT * FROM user_scores ORDER BY user_id)x) scores,
 (SELECT md5(coalesce(string_agg(row_to_json(x)::text,'#' ORDER BY id),'')) FROM (SELECT * FROM xp_history ORDER BY id)x) xp`;
try{const a=(await prod.query(sql)).rows[0],b=(await isolated.query(sql)).rows[0];
 const matches=Object.fromEntries(Object.keys(a).map(key=>[key,a[key]===b[key]]));
 console.log(JSON.stringify({production:'flashcard_english',isolated:'bivoca_m15_5_20261024',matches,allMatch:Object.values(matches).every(Boolean)},null,2));
}finally{await Promise.all([prod.end(),isolated.end()]);}
