import 'dotenv/config';
import { createHash } from 'node:crypto';
import { mkdir, readFile } from 'node:fs/promises';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const run = promisify(execFile);
const SOURCE = 'flashcard_english';
const TARGET = 'bivoca_m15_5_20261024';
const POSTGRES_BIN = 'C:\\Program Files\\PostgreSQL\\18\\bin';
const env = readDatabaseEnv(process.env);
if (env.DB_NAME !== SOURCE) throw new Error('M15.5 preparation requires the verified production database');
if (!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)) throw new Error('Only local PostgreSQL is allowed');
const dirUrl = new URL('../.m15-5-verification/', import.meta.url);
const dumpUrl = new URL('../.m15-5-verification/flashcard_english_before_m15_5.dump', import.meta.url);
const dump = fileURLToPath(dumpUrl);
await mkdir(dirUrl, { recursive: true });

const production = new pg.Client({host:env.DB_HOST,port:env.DB_PORT,database:SOURCE,user:env.DB_USER,password:env.DB_PASSWORD});
await production.connect();
try {
  const baseline=(await production.query(`SELECT
    (SELECT count(*)::int FROM flashcards) flashcards,(SELECT count(*)::int FROM topics) topics,
    (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,
    (SELECT count(*)::int FROM quiz_results) quiz_results,(SELECT count(*)::int FROM user_scores) scores,
    (SELECT count(*)::int FROM xp_history) xp`)).rows[0];
  if (baseline.flashcards!==5000||baseline.topics!==19) throw new Error(`Unexpected production baseline ${JSON.stringify(baseline)}`);
} finally { await production.end(); }

const childEnv={...process.env,PGPASSWORD:env.DB_PASSWORD};
await run(`${POSTGRES_BIN}\\pg_dump.exe`,['-h',env.DB_HOST,'-p',String(env.DB_PORT),'-U',env.DB_USER,'-d',SOURCE,'-Fc','-f',dump],{env:childEnv});
const checksum=createHash('sha256').update(await readFile(dump)).digest('hex');
const admin=new pg.Client({host:env.DB_HOST,port:env.DB_PORT,database:'postgres',user:env.DB_USER,password:env.DB_PASSWORD});
await admin.connect();
try {
  await admin.query(`SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=$1 AND pid<>pg_backend_pid()`,[TARGET]);
  await admin.query(`DROP DATABASE IF EXISTS ${TARGET}`);
  await admin.query(`CREATE DATABASE ${TARGET}`);
} finally { await admin.end(); }
await run(`${POSTGRES_BIN}\\pg_restore.exe`,['-h',env.DB_HOST,'-p',String(env.DB_PORT),'-U',env.DB_USER,'-d',TARGET,'--no-owner','--no-privileges',dump],{env:childEnv});
const isolated=new pg.Client({host:env.DB_HOST,port:env.DB_PORT,database:TARGET,user:env.DB_USER,password:env.DB_PASSWORD});
await isolated.connect();
try {
  const restored=(await isolated.query(`SELECT
    (SELECT count(*)::int FROM flashcards) flashcards,(SELECT count(*)::int FROM topics) topics,
    (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,
    (SELECT count(*)::int FROM quiz_results) quiz_results,(SELECT count(*)::int FROM user_scores) scores,
    (SELECT count(*)::int FROM xp_history) xp`)).rows[0];
  if(restored.flashcards!==5000||restored.topics!==19) throw new Error(`Restore verification failed ${JSON.stringify(restored)}`);
  console.log(JSON.stringify({source:SOURCE,target:TARGET,dump:'backend/.m15-5-verification/flashcard_english_before_m15_5.dump',checksum,restored},null,2));
} finally { await isolated.end(); }
