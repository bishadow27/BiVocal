import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as foundation from '../src/modules/quiz/quiz-session.contract.js';
const api = foundation as unknown as Record<string, any>;
test('new session contract exposes strict validators without changing legacy API', () => {
  assert.equal(typeof api.sessionCreateSchema?.safeParse, 'function');
  for (const count of [20,40,60,80,100]) assert.equal(api.sessionCreateSchema.safeParse({method:'EN_TO_VI',count,filter:{topicCode:'FOOD'}}).success,true);
  for (const count of [0,15,17,19,101]) assert.equal(api.sessionCreateSchema.safeParse({method:'EN_TO_VI',count,filter:{}}).success,false);
  for (const count of [15,17]) assert.equal(api.sessionCreateSchema.safeParse({method:'MATCHING',count,useAllAvailable:true,filter:{posCode:'POS_NOUN'}}).success,true);
  assert.equal(api.sessionCreateSchema.safeParse({method:'EN_TO_VI',count:20,filter:{topicCode:'FOOD',posCode:'POS_NOUN'}}).success,false);
  assert.equal(api.sessionCreateSchema.safeParse({method:'EN_TO_VI',count:20,filter:{},correct:true}).success,false);
});
test('answer contract rejects client grading and XP; accepts opaque IDs', () => {
  assert.equal(typeof api.sessionAnswerSchema?.safeParse,'function');
  const request={actionId:'550e8400-e29b-41d4-a716-446655440000',questionId:'650e8400-e29b-41d4-a716-446655440000',selectedOptionId:'750e8400-e29b-41d4-a716-446655440000'};
  assert.equal(api.sessionAnswerSchema.safeParse(request).success,true);
  for(const extra of [{correct:true},{awardedXp:10},{flashcardId:1}]) assert.equal(api.sessionAnswerSchema.safeParse({...request,...extra}).success,false);
});
test('test guard rejects production URL, backend port and unverified instance identity', () => {
  assert.equal(typeof api.assertIsolatedQuizTarget,'function');
  const safe={host:'127.0.0.1',port:55433,database:'bivoca_m16_7_quiz_test',backendPort:3001,systemIdentifier:'1234567890',expectedSystemIdentifier:'1234567890',connectionUrl:'postgresql://test@127.0.0.1:55433/bivoca_m16_7_quiz_test'};
  assert.doesNotThrow(()=>api.assertIsolatedQuizTarget(safe));
  for(const patch of [{port:5432},{database:'bivoca_m15_7_recovered'},{host:'localhost'},{backendPort:3000},{systemIdentifier:'other'},{expectedSystemIdentifier:''},{connectionUrl:'postgresql://test@127.0.0.1:5432/bivoca_m15_7_recovered'},{connectionUrl:safe.connectionUrl+'?host=127.0.0.1&port=5432'}])assert.throws(()=>api.assertIsolatedQuizTarget({...safe,...patch}));
});
