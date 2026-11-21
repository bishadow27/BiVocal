import assert from 'node:assert/strict';
import { describe,it } from 'node:test';
import type { Pool } from 'pg';
import { createAdminRepository } from '../src/modules/admin/admin.repository.js';
import type { CardInput } from '../src/modules/admin/admin.schema.js';

const input:CardInput={topicCode:'B',english:'fixture',vietnamese:'kiểm thử',pronunciation:null,partOfSpeech:'noun',example:null,exampleVi:null,level:'A1'};
const dto={id:10,topic_id:2,topic_code:'B',topic_name:'B',english:'fixture',vietnamese:'kiểm thử',pronunciation:null,part_of_speech:'noun',example:null,example_vi:null,level:'A1',is_active:false,created_at:null,updated_at:null};
type Step={match:RegExp;rows?:unknown[];fail?:boolean;params?:unknown[]};
function fixture(steps:Step[],commitFail=false,rollbackFail=false){
 const calls:{sql:string;params:unknown[]}[]=[];let releaseArg:unknown='NOT_RELEASED';
 const client={async query(sql:string,params:unknown[]=[]){calls.push({sql,params});if(sql==='BEGIN')return{rows:[]};if(sql==='ROLLBACK'){if(rollbackFail)throw Error('rollback failure');return{rows:[]};}if(sql==='COMMIT'){if(commitFail)throw Error('commit failure');assert.equal(steps.length,0);return{rows:[]};}const s=steps.shift();assert.ok(s,'unexpected query: '+sql);assert.match(sql,s.match);if(s.params)assert.deepEqual(params,s.params);if(s.fail)throw Error('injected operation failure');return{rows:s.rows??[]};},release(arg?:unknown){releaseArg=arg;}};
 const pool={async connect(){return client;},async query(){throw Error('MUST_USE_TRANSACTION_CLIENT');}} as unknown as Pool;
 return{repository:createAdminRepository(pool),calls,get released(){return releaseArg;},steps};
}
const topics:Step={match:/SELECT id,code FROM public\.topics.*FOR KEY SHARE/s,rows:[{id:2,code:'B'},{id:40,code:'POS_NOUN'},{id:41,code:'POS_VERB'}]};
const lock=(links:unknown[]=[{topic_id:1,is_primary:true,code:'A'},{topic_id:3,is_primary:false,code:'C'}],pos:string|null='noun')=>[
 {match:/SELECT id,topic_id,part_of_speech FROM public\.flashcards.*FOR UPDATE/s,params:[10],rows:[{id:10,topic_id:1,part_of_speech:pos}]},
 {match:/FROM public\.flashcard_topics ft.*ORDER BY ft.topic_id FOR UPDATE OF ft/s,params:[10],rows:links},{...topics,rows:topics.rows!.map(r=>({...r as object}))}] satisfies Step[];
const update:Step={match:/UPDATE public\.flashcards SET topic_id=\$2/};
const primary:Step={match:/INSERT INTO public\.flashcard_topics.*ON CONFLICT \(flashcard_id,topic_id\) DO UPDATE SET is_primary=TRUE/s,params:[10,2]};
const result:Step={match:/SELECT f\.id,.*FROM public\.flashcards f.*WHERE f.id=\$1/s,params:[10],rows:[dto]};
const removeA:Step={match:/DELETE FROM public\.flashcard_topics WHERE flashcard_id=\$1 AND topic_id=\$2/,params:[10,1]};

describe('M16.8 Admin membership transaction unit contracts (scripted client, not PostgreSQL)',()=>{
 it('create card, primary and mapped POS share one transaction; return only after COMMIT',async()=>{
  const f=fixture([topics,{match:/INSERT INTO public\.flashcards/,rows:[{id:10}]},{match:/INSERT INTO public\.flashcard_topics/,params:[10,2]},{match:/INSERT INTO public\.flashcard_topics/,params:[10,40]},result]);
  assert.equal((await f.repository.createCard({...input,isActive:false}))?.id,10);assert.equal(f.calls[0]?.sql,'BEGIN');assert.equal(f.calls.at(-1)?.sql,'COMMIT');assert.notEqual(f.released,'NOT_RELEASED');
 });
 it('A2 removes only old A, promotes B and keeps other secondary links',async()=>{
  const f=fixture([...lock(),update,removeA,primary,result]);await f.repository.updateCard(10,input);
  const deletes=f.calls.filter(c=>c.sql.startsWith('DELETE'));assert.deepEqual(deletes.map(c=>c.params),[[10,1]]);assert.doesNotMatch(f.calls.find(c=>c.sql.startsWith('UPDATE'))!.sql,/is_active\s*=/);
 });
 it('B already secondary is promoted with conflict update, not duplicated',async()=>{
  const f=fixture([...lock([{topic_id:1,is_primary:true,code:'A'},{topic_id:2,is_primary:false,code:'B'},{topic_id:3,is_primary:false,code:'C'}]),update,removeA,primary,result]);await f.repository.updateCard(10,input);assert.equal(f.calls.filter(c=>c.sql.startsWith('INSERT INTO public.flashcard_topics')).length,1);
 });
 it('A to A preserves all membership and inactive status',async()=>{
  const f=fixture([...lock([{topic_id:1,is_primary:true,code:'A'},{topic_id:3,is_primary:false,code:'C'}]),update,{...primary,params:[10,1]},result]);f.steps[2]!.rows=[{id:1,code:'A'},{id:40,code:'POS_NOUN'}];await f.repository.updateCard(10,{...input,topicCode:'A'});assert.ok(!f.calls.some(c=>c.sql.startsWith('DELETE')));
 });
 it('POS change removes only unsuitable POS secondary and inserts mapped verb',async()=>{
  const f=fixture([...lock(),update,removeA,primary,{match:/DELETE FROM public\.flashcard_topics ft.*NOT ft.is_primary.*t.code<>\$2/s,params:[10,'POS_VERB']},{match:/INSERT INTO public\.flashcard_topics/,params:[10,41]},result]);await f.repository.updateCard(10,{...input,partOfSpeech:'verb'});
 });
 it('null POS removes old POS secondary without PHRASE fallback',async()=>{
  const f=fixture([...lock(),update,removeA,primary,{match:/DELETE FROM public\.flashcard_topics ft.*NOT ft.is_primary/s,params:[10,null]},result]);await f.repository.updateCard(10,{...input,partOfSpeech:null});assert.ok(f.calls.every(c=>!c.params.includes('POS_PHRASE')));
 });
 it('invalid POS rejects without any transaction or fallback',async()=>{
  const f=fixture([]);await assert.rejects(f.repository.createCard({...input,partOfSpeech:'unknown' as CardInput['partOfSpeech'],isActive:true}),{code:'ADMIN_POS_INVALID'});assert.equal(f.calls.length,0);
 });
 for(const [name,links]of [['missing',[]],['drift',[{topic_id:3,is_primary:true,code:'C'}]],['multiple',[{topic_id:1,is_primary:true,code:'A'},{topic_id:3,is_primary:true,code:'C'}]],['duplicate',[{topic_id:1,is_primary:true,code:'A'},{topic_id:1,is_primary:false,code:'A'}]]] as const){
  it('rejects '+name+' primary/membership state instead of silently repairing',async()=>{const f=fixture(lock([...links]));await assert.rejects(f.repository.updateCard(10,input),{code:'ADMIN_TOPIC_MEMBERSHIP_DRIFT'});assert.equal(f.calls.at(-1)?.sql,'ROLLBACK');assert.ok(!f.calls.some(c=>/^(UPDATE|INSERT|DELETE)/.test(c.sql)));});
 }
 it('conflicting POS primary fails before card write',async()=>{const f=fixture([...lock()]);f.steps[2]!.rows=[{id:2,code:'POS_VERB'},{id:40,code:'POS_NOUN'}];await assert.rejects(f.repository.updateCard(10,{...input,topicCode:'POS_VERB'}),{code:'ADMIN_PRIMARY_POS_CONFLICT'});assert.equal(f.calls.at(-1)?.sql,'ROLLBACK');});
 it('missing target topic rolls back',async()=>{const f=fixture([...lock()]);f.steps[2]!.rows=[];await assert.rejects(f.repository.updateCard(10,input),{code:'ADMIN_TOPIC_NOT_FOUND'});assert.equal(f.calls.at(-1)?.sql,'ROLLBACK');});
 for(const at of [3,4,5,6])it('operation failure at step '+at+' rolls back whole transaction',async()=>{const steps:Step[]=[...lock(),update,removeA,primary,result].map(s=>({...s}));steps[at]!.fail=true;const f=fixture(steps);await assert.rejects(f.repository.updateCard(10,input),/injected/);assert.equal(f.calls.at(-1)?.sql,'ROLLBACK');assert.ok(!f.calls.some(c=>c.sql==='COMMIT'));});
 it('commit failure never returns success and rollback failure discards connection',async()=>{const f=fixture([...lock(),update,removeA,primary,result],true,true);await assert.rejects(f.repository.updateCard(10,input),/commit failure/);assert.equal(f.released,true);});
 it('concurrent callers each lock card BEFORE reading locked memberships (SQL contract only)',async()=>{const clients=[fixture([...lock(),update,removeA,primary,result]),fixture([...lock(),update,removeA,primary,result])];await Promise.all(clients.map(c=>c.repository.updateCard(10,input)));for(const f of clients){assert.match(f.calls[1]!.sql,/FOR UPDATE/);assert.match(f.calls[2]!.sql,/FOR UPDATE OF ft/);}});
});
it('create failure after primary INSERT rolls back card and memberships',async()=>{const f=fixture([topics,{match:/INSERT INTO public\.flashcards/,rows:[{id:10}]},{match:/INSERT INTO public\.flashcard_topics/,params:[10,2]},{match:/INSERT INTO public\.flashcard_topics/,fail:true}]);await assert.rejects(f.repository.createCard({...input,isActive:true}),/injected/);assert.equal(f.calls.at(-1)?.sql,'ROLLBACK');assert.ok(!f.calls.some(c=>c.sql==='COMMIT'));});
it('matching POS primary is a single link, not also duplicated as secondary',async()=>{const f=fixture([{...topics,rows:[{id:40,code:'POS_NOUN'}]},{match:/INSERT INTO public\.flashcards/,rows:[{id:10}]},{match:/INSERT INTO public\.flashcard_topics/,params:[10,40]},result]);await f.repository.createCard({...input,topicCode:'POS_NOUN',isActive:true});assert.equal(f.calls.filter(c=>c.sql.startsWith('INSERT INTO public.flashcard_topics')).length,1);});
it('mapped POS topic missing aborts instead of silently omitting POS',async()=>{const f=fixture([{...topics,rows:[{id:2,code:'B'}]}]);await assert.rejects(f.repository.createCard({...input,isActive:true}),{code:'ADMIN_POS_TOPIC_MISSING'});assert.equal(f.calls.at(-1)?.sql,'ROLLBACK');});
it('primary POS conflict on create rolls back without card insertion',async()=>{const f=fixture([{...topics,rows:[{id:2,code:'POS_VERB'},{id:40,code:'POS_NOUN'}]}]);await assert.rejects(f.repository.createCard({...input,topicCode:'POS_VERB',isActive:true}),{code:'ADMIN_PRIMARY_POS_CONFLICT'});assert.ok(!f.calls.some(c=>c.sql.startsWith('INSERT')));});
