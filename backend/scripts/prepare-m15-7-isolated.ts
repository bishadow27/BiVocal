import 'dotenv/config';
import { createHash } from 'node:crypto';
import { mkdir, readFile } from 'node:fs/promises';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const run=promisify(execFile),SOURCE='flashcard_english',TARGET='bivoca_m15_7_20261025',POSTGRES_BIN='C:\\Program Files\\PostgreSQL\\18\\bin';
const env=readDatabaseEnv(process.env);if(env.DB_NAME!==SOURCE||!['127.0.0.1','localhost','::1'].includes(env.DB_HOST))throw new Error('M15.7 preparation requires local production');
const dir=new URL('../.m15-7-verification/',import.meta.url),dumpUrl=new URL('../.m15-7-verification/flashcard_english_before_m15_7_stage.dump',import.meta.url);await mkdir(dir,{recursive:true});const dump=fileURLToPath(dumpUrl);
const config={host:env.DB_HOST,port:env.DB_PORT,user:env.DB_USER,password:env.DB_PASSWORD};
const snapshotSql=`SELECT (SELECT count(*)::int FROM flashcards) flashcards,(SELECT count(*)::int FROM topics) topics,
 (SELECT count(*)::int FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.code LIKE 'POS\\_%' ESCAPE '\\') pos,
 (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,(SELECT count(*)::int FROM quiz_results) quiz,
 (SELECT count(*)::int FROM user_scores) scores,(SELECT count(*)::int FROM xp_history) xp`;
const production=new pg.Client({...config,database:SOURCE});await production.connect();let baseline;
try{baseline=(await production.query(snapshotSql)).rows[0];if(baseline.flashcards!==20000||baseline.topics!==38||baseline.pos!==12858)throw new Error(`Unexpected production baseline ${JSON.stringify(baseline)}`);}finally{await production.end();}
const childEnv={...process.env,PGPASSWORD:env.DB_PASSWORD};await run(`${POSTGRES_BIN}\\pg_dump.exe`,['-h',env.DB_HOST,'-p',String(env.DB_PORT),'-U',env.DB_USER,'-d',SOURCE,'-Fc','-f',dump],{env:childEnv});
const checksum=createHash('sha256').update(await readFile(dump)).digest('hex'),admin=new pg.Client({...config,database:'postgres'});await admin.connect();
try{await admin.query('SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=$1 AND pid<>pg_backend_pid()',[TARGET]);await admin.query(`DROP DATABASE IF EXISTS ${TARGET}`);await admin.query(`CREATE DATABASE ${TARGET}`);}finally{await admin.end();}
await run(`${POSTGRES_BIN}\\pg_restore.exe`,['-h',env.DB_HOST,'-p',String(env.DB_PORT),'-U',env.DB_USER,'-d',TARGET,'--no-owner','--no-privileges',dump],{env:childEnv});
const isolated=new pg.Client({...config,database:TARGET});await isolated.connect();try{const restored=(await isolated.query(snapshotSql)).rows[0];if(JSON.stringify(restored)!==JSON.stringify(baseline))throw new Error(`Restore mismatch ${JSON.stringify(restored)}`);console.log(JSON.stringify({source:SOURCE,target:TARGET,dump:'backend/.m15-7-verification/flashcard_english_before_m15_7_stage.dump',checksum,restored},null,2));}finally{await isolated.end();}
