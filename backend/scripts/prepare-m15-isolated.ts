import 'dotenv/config';
import { mkdir } from 'node:fs/promises';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const run=promisify(execFile); const SOURCE='flashcard_english'; const TARGET='bivoca_m15_20261024';
const bin='C:\\Program Files\\PostgreSQL\\18\\bin';
const env=readDatabaseEnv(process.env);
if(env.DB_NAME!==SOURCE) throw new Error('M15 preparation must read only the verified production database name');
const dumpUrl=new URL('../.m15-verification/flashcard_english_before_m15.dump',import.meta.url);
const dump=fileURLToPath(dumpUrl);
await mkdir(new URL('../.m15-verification/',import.meta.url),{recursive:true});
const admin=new pg.Client({host:env.DB_HOST,port:env.DB_PORT,database:'postgres',user:env.DB_USER,password:env.DB_PASSWORD});
await admin.connect();
try{
 await admin.query(`SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=$1 AND pid<>pg_backend_pid()`,[TARGET]);
 await admin.query(`DROP DATABASE IF EXISTS ${TARGET}`); await admin.query(`CREATE DATABASE ${TARGET}`);
}finally{await admin.end();}
const childEnv={...process.env,PGPASSWORD:env.DB_PASSWORD};
await run(`${bin}\\pg_dump.exe`,['-h',env.DB_HOST,'-p',String(env.DB_PORT),'-U',env.DB_USER,'-d',SOURCE,'-Fc','-f',dump],{env:childEnv});
await run(`${bin}\\pg_restore.exe`,['-h',env.DB_HOST,'-p',String(env.DB_PORT),'-U',env.DB_USER,'-d',TARGET,'--no-owner','--no-privileges',dump],{env:childEnv});
console.log(JSON.stringify({source:SOURCE,target:TARGET,dump:'backend/.m15-verification/flashcard_english_before_m15.dump'}));
