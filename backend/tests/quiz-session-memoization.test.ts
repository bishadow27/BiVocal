import { test } from 'node:test';
import assert from 'node:assert/strict';
import { preparePool, availability, type Card, type Method } from '../src/modules/quiz/quiz-session.generator.js';
import * as baseline from './support/quiz-session-generator-baseline.js';
const methods: Method[] = ['MIXED','CHALLENGE','CONTEXT','EN_TO_VI','VI_TO_EN','MATCHING'];
const card = (id:number):Card => ({id,english:'term'+id,vietnamese:'meaning'+id,
 example:'This is term'+id+' at location '+id+'.',eligible:true,topic_codes:['TEST'],
 pronunciation:null,part_of_speech:'noun',level:'A1'});
function differential(cards:Card[]) {
 const snapshot=JSON.stringify(cards);
 for(const method of methods) {
  const expected=baseline.preparePool(cards,method),actual=preparePool(cards,method);
  assert.equal(actual.length,expected.length,method);
  actual.forEach((value,index)=>assert.strictEqual(value,expected[index],method+' ordered object identity'));
  assert.deepEqual(availability(cards,method),baseline.availability(cards,method),method+' capacity/hash');
 }
 assert.equal(JSON.stringify(cards),snapshot,'input remains unchanged');
}
test('V2 preparePool normalizes each source card a bounded number of times per call',()=>{
 const cards=Array.from({length:100},(_,i)=>card(i+1));
 const original=String.prototype.normalize;
 for(const method of methods) {
  let calls=0;
  String.prototype.normalize=function(this:string,form?:string){calls++;return original.call(this,form);};
  try {preparePool(cards,method);} finally {String.prototype.normalize=original;}
  assert.ok(calls<=cards.length*12,method+': '+calls+' normalization calls exceeds12percard');
 }
});
test('V2 memoization preserves ordered eligibility and availability hashes on ambiguity cases',()=>{
 differential([]);
 const cases:Partial<Card>[]=[
  {},{english:'ＴＥＲＭ1'},{vietnamese:'MEANING1.!?'},{vietnamese:'meaning4; shared (note)'},
  {vietnamese:'shared / other'},{vietnamese:'third|other'},{vietnamese:'fourth, fifth'},
  {english:' “Hello” ',example:'Say “Hello” once.'},{english:"'hello'",example:"Say 'hello' once."},
  {example:null},{example:'term11 term11'},{example:'term120'},
  {english:' '},{vietnamese:' '},{eligible:false},
  {english:'alpha',example:'An alpha exists.'},{english:'beta',example:'An beta exists.'},
  {english:'meaning18'},{vietnamese:'term19'},
 ];
 differential(cases.map((changes,i)=>({...card(i+1),...changes})));
 const duplicateIds=Array.from({length:12},(_,i)=>card(i+1));
 duplicateIds[1]!.id=duplicateIds[0]!.id;
 differential(duplicateIds);
 differential(Array.from({length:12},(_,i)=>({...card(i+1),vietnamese:'shared; meaning'+i})));
});
test('V2 memoization matches baseline across deterministic fuzz and input permutations',()=>{
 let seed=168;
 const random=()=>{seed=(Math.imul(seed,1664525)+1013904223)>>>0;return seed/4294967296;};
 for(let sample=0;sample<60;sample++) {
  const cards=Array.from({length:Math.floor(random()*25)},(_,i)=>{
   const value=card(i+1),choice=Math.floor(random()*8);
   if(choice===0)value.vietnamese='shared'+Math.floor(random()*4)+'; extra'+i;
   if(choice===1)value.english='duplicate'+Math.floor(random()*4);
   if(choice===2)value.example=null;
   if(choice===3)value.eligible=false;
   if(choice===4)value.vietnamese='term'+Math.floor(random()*10);
   if(choice===5)value.example='This is '+value.english+' twice '+value.english;
   if(choice===6)value.vietnamese='Meaning'+i+' (note) / shared'+Math.floor(random()*4);
   return value;
  });
  differential(cards);
  differential([...cards].reverse());
 }
});
test('V2 memoization sees same-object mutations between calls without stale cache',()=>{
 const cards=Array.from({length:20},(_,i)=>card(i+1));
 differential(cards);
 cards[0]!.english=cards[1]!.english;
 differential(cards);
 cards[0]!.english='replacement';cards[0]!.example='A replacement exists.';
 cards[2]!.vietnamese=cards[3]!.vietnamese;cards[4]!.eligible=false;
 differential(cards);
 cards[5]!.example='The term6 appears twice: term6';
 differential(cards);
});
