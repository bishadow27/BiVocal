import 'dotenv/config';
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

type Candidate = Record<string, unknown> & {
  candidateId:string; headword:string; vietnamese:string; partOfSpeech:string|null;
  cefr:string|null; primaryTopicCode:string; topicCodes:string[]; validationReasons:string[];
  source:Record<string, unknown>;
};

const env=readDatabaseEnv(process.env);
if(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST))throw new Error('Only local PostgreSQL is allowed');
const proposalUrl=new URL('../datasets/processed/m15_5/repair_v2_proposed_additions_15000_unverified.jsonl',import.meta.url);
const reserveUrl=new URL('../datasets/processed/m15_5/approved.jsonl',import.meta.url);
const blockUrl=new URL('../datasets/quality/m15_5/repair_v2/hard_block_only.csv',import.meta.url);
const outputUrl=new URL('../.m15-5-verification/m15-5-unverified-plan.jsonl',import.meta.url);
const summaryUrl=new URL('../.m15-5-verification/m15-5-unverified-plan-summary.json',import.meta.url);
const parse=(text:string)=>text.split(/\r?\n/).filter(Boolean).map(line=>JSON.parse(line) as Candidate);
const proposal=parse(await readFile(proposalUrl,'utf8'));
const reserve=parse(await readFile(reserveUrl,'utf8'));
const blockIds=new Set((await readFile(blockUrl,'utf8')).split(/\r?\n/).slice(1).filter(Boolean).map(line=>line.split(',')[0]!));
const topics=JSON.parse(await readFile(new URL('../datasets/processed/m15_5/topics.json',import.meta.url),'utf8')) as Array<{code:string}>;
const topicCodes=new Set(topics.map(topic=>topic.code));
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:1});

const normalized=(value:string)=>value.trim().toLocaleLowerCase('vi-VN');
const sense=(candidate:Candidate)=>`${normalized(candidate.headword)}\u001f${normalized(candidate.vietnamese)}`;
function valid(candidate:Candidate):boolean {
  return Boolean(candidate.candidateId?.trim()&&candidate.headword?.trim()&&candidate.vietnamese?.trim()&&
    candidate.partOfSpeech?.trim()&&candidate.primaryTopicCode&&topicCodes.has(candidate.primaryTopicCode)&&
    Array.isArray(candidate.topicCodes)&&candidate.topicCodes.includes(candidate.primaryTopicCode)&&
    candidate.topicCodes.every(code=>topicCodes.has(code))&&candidate.source?.name&&candidate.source?.url&&
    candidate.source?.version&&candidate.source?.license&&!blockIds.has(candidate.candidateId));
}

try{
  const database=(await pool.query<{name:string}>('SELECT current_database() name')).rows[0]!.name;
  const baseline=(await pool.query<{cards:number;topics:number}>(`SELECT
    (SELECT count(*)::int FROM flashcards) cards,(SELECT count(*)::int FROM topics) topics`)).rows[0]!;
  if(database!==env.DB_NAME||baseline.cards!==5000||baseline.topics!==19)
    throw new Error(`Plan requires a verified 5000-card/19-topic baseline, got ${JSON.stringify({database,...baseline})}`);
  const existing=new Set((await pool.query<{key:string}>(`SELECT lower(trim(english))||chr(31)||lower(trim(vietnamese)) key FROM flashcards`)).rows.map(row=>row.key));
  const selected:Candidate[]=[];const seenIds=new Set<string>();const seenSenses=new Set(existing);
  let invalid=0,blocked=0,duplicates=0;
  for(const candidate of [...proposal,...reserve]){
    if(blockIds.has(candidate.candidateId)){blocked++;continue;}
    if(!valid(candidate)){invalid++;continue;}
    const key=sense(candidate);
    if(seenIds.has(candidate.candidateId)||seenSenses.has(key)){duplicates++;continue;}
    seenIds.add(candidate.candidateId);seenSenses.add(key);
    const copy={...candidate};delete copy.normalizedKey;
    const reasons=[...new Set([...(candidate.validationReasons??[]),'IMPORTED_UNVERIFIED','SEMANTIC_REVIEW_PENDING'])].sort();
    copy.validationStatus='NEEDS_REVIEW';copy.validationReasons=reasons;copy.importEligible=true;
    copy.quizEligible=false;copy.reviewClassification='IMPORTED_UNVERIFIED';copy.contentStatus='IMPORTED_UNVERIFIED';
    copy.source={...candidate.source,semanticVerified:false,independentMeaningConfirmed:false};
    selected.push(copy);if(selected.length===15000)break;
  }
  if(selected.length!==15000)throw new Error(`Only ${selected.length} technically eligible candidates are available`);
  await mkdir(fileURLToPath(new URL('../.m15-5-verification/',import.meta.url)),{recursive:true});
  const text=selected.map(candidate=>JSON.stringify(candidate)).join('\n')+'\n';
  const sha256=createHash('sha256').update(text).digest('hex');
  await writeFile(outputUrl,text,'utf8');
  const summary={database,baseline,proposalCandidates:proposal.length,reserveCandidates:reserve.length,
    selected:selected.length,fromProposal:selected.filter(c=>proposal.some(p=>p.candidateId===c.candidateId)).length,
    invalid,blocked,duplicates,sha256,status:'IMPORTED_UNVERIFIED',quizEligible:false};
  await writeFile(summaryUrl,JSON.stringify(summary,null,2)+'\n','utf8');
  console.log(JSON.stringify(summary,null,2));
}finally{await pool.end();}
