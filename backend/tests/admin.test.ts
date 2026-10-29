import assert from 'node:assert/strict';
import { describe,it,beforeEach,afterEach } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { AuthModule } from '../src/modules/auth/auth.service.js';
import { adminDouble } from './support/admin-double.js';
import { parseRoleArguments } from '../src/modules/admin/admin.schema.js';
import { createAdminService } from '../src/modules/admin/admin.service.js';

const secret='admin-unit-test-synthetic-secret-over-32-characters';
const tokens=createTokenService(secret,3600);
const auth:AuthModule={tokens,service:{async register(){throw new Error('Unused');},async login(){throw new Error('Unused');},async currentUser(){throw new Error('Unused');}} as unknown as AuthModule['service']};
const topic={code:'BUSINESS',name:' Business ',description:null,imageUrl:null};
const card={topicCode:'FOOD',english:' milk ',vietnamese:' sữa ',level:'A1'};
const endpoints:[string,string,unknown?][]=[['/me','GET'],['/dashboard','GET'],['/topics','GET'],['/topics','POST',topic],
  ['/topics/1','PUT',topic],['/flashcards','GET'],['/flashcards','POST',card],['/flashcards/1','PUT',card],
  ['/flashcards/1/status','PATCH',{isActive:false}],['/users','GET'],['/users/42','GET'],['/users/42/progress','GET'],
  ['/users/42/quiz-history','GET'],['/users/42/role','PATCH',{role:'ADMIN'}],['/stats/quiz','GET'],['/leaderboard','GET']];

describe('Milestone 6A Admin HTTP (real JWT; repository doubles; no database)',()=>{
  let server:Server,base:string,f:ReturnType<typeof adminDouble>;
  beforeEach(async()=>{
    f=adminDouble();server=createApp({admin:f.repository,database:{async ping(){}},
      vocabularyReview:{async stats(){throw Object.assign(new Error('private missing table'),{code:'42P01'});}} as any,
      topics:{async findAll(){return[];},async findCatalog(){return[];},async findByCode(){return undefined;}},
      flashcards:{async findById(id){const c=f.cards.get(id);return c?.is_active?c:undefined;},
        async findByTopicCode(code){return[...f.cards.values()].filter(c=>c.is_active&&c.topic_code===code);},
        async search(){return {rows:[],total:0};}}},auth).listen(0,'127.0.0.1');
    await new Promise<void>((resolve,reject)=>{server.once('listening',resolve);server.once('error',reject);});
    base=`http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
  });
  afterEach(async()=>{await new Promise<void>((resolve,reject)=>{server.close(e=>e?reject(e):resolve());server.closeAllConnections();});});
  const request=(path:string,method='GET',body?:unknown,token:string|null=tokens.sign(41))=>fetch(base+'/admin'+path,
    {method,headers:{...(token===null?{}:{Authorization:`Bearer ${token}`}),...(body===undefined?{}:{'Content-Type':'application/json'}),'X-User-Id':'41','X-Role':'ADMIN'},
      ...(body===undefined?{}:{body:JSON.stringify(body)})});
  async function good(path:string,method='GET',body?:unknown,status=200){const r=await request(path,method,body);assert.equal(r.status,status);return r.json();}
  async function error(path:string,method:string,body:unknown,status:number,code?:string){const r=await request(path,method,body);assert.equal(r.status,status);
    const result=await r.json();if(code)assert.equal(result.error.code,code);return result;}
  for(const [name,token] of [['missing',null],['invalid','bad'],['expired',jwt.sign({sub:'41',exp:1},secret)]] as const){
    it(`${name} JWT rejected on all16 admin routes before DB role lookup`,async()=>{
      for(const [path,method,body]of endpoints)assert.equal((await request(path,method,body,token)).status,401);assert.deepEqual(f.calls,[]);
    });
  }
  it('normal USER denied every admin route even with spoofed query/header/role claim',async()=>{
    const token=jwt.sign({sub:'42',role:'ADMIN'},secret,{expiresIn:3600});
    for(const [path,method,body]of endpoints){const r=await request(path+'?userId=41&role=ADMIN',method,body,token);assert.equal(r.status,403);
      assert.deepEqual(await r.json(),{error:{code:'ADMIN_FORBIDDEN',message:'Administrator access required'}});}
    assert.ok(f.calls.every(c=>c.name==='account'&&c.args[0]===42));
  });
  it('missing/deleted user gets401 and authorization lookup errors stay generic',async()=>{
    f.users.delete(41);await error('/me','GET',undefined,401,'AUTH_USER_NOT_FOUND');
    f.fail('account');const result=await error('/me','GET',undefined,500,'INTERNAL_SERVER_ERROR');assert.doesNotMatch(JSON.stringify(result),/SQL|password_hash|JWT_SECRET/);
  });
  it('admin/me exposes only own public admin fields and queries role each request',async()=>{
    Object.assign(f.users.get(41)!,{password_hash:'secret',token:'secret'});
    assert.deepEqual(await good('/me'),{id:41,username:'user41',email:'user41@example.com',fullName:'User 41',avatarUrl:null,role:'ADMIN'});
    await good('/me');assert.equal(f.calls.filter(c=>c.name==='account').length,2);
  });
  it('review endpoints identify a missing M13.6 schema without exposing PostgreSQL details',async()=>{
    const result=await error('/vocabulary-review/stats','GET',undefined,503,'REVIEW_SCHEMA_UNAVAILABLE');
    assert.deepEqual(result,{error:{code:'REVIEW_SCHEMA_UNAVAILABLE',message:'Vocabulary review database schema is not available'}});
    assert.doesNotMatch(JSON.stringify(result),/relation|SQL|table|private/i);
  });
  it('promote/demote takes effect immediately for the same unchanged JWT',async()=>{
    const token=tokens.sign(42);assert.equal((await request('/me','GET',undefined,token)).status,403);
    await good('/users/42/role','PATCH',{role:'ADMIN'});assert.equal((await request('/me','GET',undefined,token)).status,200);
    await good('/users/42/role','PATCH',{role:'USER'});assert.equal((await request('/me','GET',undefined,token)).status,403);
  });
  it('self-demotion blocked; self ADMIN no-op and changing another account allowed',async()=>{
    await error('/users/41/role','PATCH',{role:'USER'},409,'ADMIN_SELF_ROLE_CHANGE_FORBIDDEN');
    assert.ok(!f.calls.some(c=>c.name==='setRole'));assert.equal((await good('/users/41/role','PATCH',{role:'ADMIN'})).role,'ADMIN');
  });
  it('authorizes before parsing invalid JSON mutation body',async()=>{
    const r=await fetch(base+'/admin/topics',{method:'POST',headers:{Authorization:`Bearer ${tokens.sign(42)}`,'Content-Type':'application/json'},body:'{'});
    assert.equal(r.status,403);
  });
  it('dashboard returns all-time inventory/XP and scoped quiz aggregates with rounded accuracy',async()=>{
    f.stats.totals={total_answers:'3',correct_answers:'2',wrong_answers:'1'};
    f.stats.byTopic=[{code:'FOOD',name:'Food',...f.stats.totals}];f.stats.byDay=[{date:'2026-10-05',answers:'3',correct:'2',wrong:'1'}];
    f.users.get(42)!.total_xp=250;f.board.push({rank:'1',user_id:42,username:'user42',full_name:null,avatar_url:null,total_xp:250,current_streak:2,longest_streak:3});
    const d=await good('/dashboard?range=30d');assert.deepEqual(d.users,{total:2});assert.deepEqual(d.flashcards,{total:1,active:1,inactive:0});
    assert.equal(d.quiz.accuracy,66.67);assert.equal(d.gamification.totalXp,250);assert.equal(d.topUsers[0].level,3);
    assert.deepEqual(d.topTopics,[{code:'FOOD',name:'Food',quizCount:3}]);assert.equal(d.quizByDay[0].answers,3);
    assert.deepEqual(f.calls.find(c=>c.name==='dashboard')!.args,['30d']);assert.doesNotMatch(JSON.stringify(d),/email|password|avatarUrl/);
  });
  it('dashboard zero activity has null accuracy and empty tops; default range7d',async()=>{
    const d=await good('/dashboard');assert.equal(d.quiz.accuracy,null);assert.equal(d.gamification.totalXp,0);
    assert.deepEqual(d.topUsers,[]);assert.deepEqual(d.topTopics,[]);assert.deepEqual(f.calls.find(c=>c.name==='dashboard')!.args,['7d']);
  });
  it('dashboard service handles a completely empty database with zero totals',async()=>{
    f.users.clear();f.cards.clear();f.topics.clear();
    assert.deepEqual(await createAdminService(f.repository).dashboard('all'),{
      users:{total:0},flashcards:{total:0,active:0,inactive:0},
      quiz:{totalAnswers:0,correctAnswers:0,wrongAnswers:0,accuracy:null},gamification:{totalXp:0},
      topUsers:[],topTopics:[],quizByDay:[],
    });
  });
  it('topics support case-insensitive search, trim and pagination including empty later pages',async()=>{
    await good('/topics','POST',topic,201);const page=await good('/topics?limit=1&page=2');assert.equal(page.total,2);assert.equal(page.totalPages,2);assert.equal(page.items.length,1);
    assert.equal((await good('/topics?search=%20business%20')).items[0].name,'Business');
    assert.deepEqual(await good('/topics?page=9'),{items:[],page:9,limit:20,total:2,totalPages:1});
  });
  it('topic creation/update returns specified DTO, maps duplicate code and missing ID',async()=>{
    const t=await good('/topics','POST',topic,201);assert.deepEqual(t,{id:2,code:'BUSINESS',name:'Business',description:null,imageUrl:null,flashcardCount:0});
    await error('/topics','POST',topic,409,'TOPIC_CODE_EXISTS');
    assert.equal((await good('/topics/2','PUT',{...topic,name:'Updated'})).name,'Updated');
    await error('/topics/999','PUT',topic,404,'ADMIN_TOPIC_NOT_FOUND');
  });
  it('baseline unique topic name errors are safe conflicts',async()=>{
    f.fail('createTopic',{code:'23505',constraint:'topics_name_key',detail:'private SQL'});
    await error('/topics','POST',topic,409,'TOPIC_NAME_EXISTS');
  });
  it('flashcards create trimmed fields/default active, filter search/topic/status/level and paginate',async()=>{
    const c=await good('/flashcards','POST',card,201);assert.equal(c.english,'milk');assert.equal(c.vietnamese,'sữa');assert.equal(c.isActive,true);
    assert.equal((await good('/flashcards?search=MILK&topicCode=FOOD&status=ACTIVE&level=A1')).total,1);
    assert.equal((await good('/flashcards?status=INACTIVE')).total,0);assert.equal((await good('/flashcards?page=2&limit=1')).items[0].id,c.id);
  });
  it('flashcard update preserves status; dedicated status endpoint hides card from public detail',async()=>{
    await good('/flashcards/1/status','PATCH',{isActive:false});
    assert.equal((await fetch(base+'/flashcards/1')).status,404);
    const c=await good('/flashcards/1','PUT',card);assert.equal(c.isActive,false);
    await error('/flashcards/1','PUT',{...card,isActive:true},400,'VALIDATION_ERROR');
    await good('/flashcards/1/status','PATCH',{isActive:true});assert.equal((await fetch(base+'/flashcards/1')).status,200);
  });
  it('duplicate card maps409, missing topic/card maps404 without leaking driver details',async()=>{
    await error('/flashcards','POST',{...card,english:'APPLE',vietnamese:'TÁO'},409,'FLASHCARD_EXISTS');
    await error('/flashcards','POST',{...card,topicCode:'MISSING'},404,'ADMIN_TOPIC_NOT_FOUND');
    await error('/flashcards/999','PUT',card,404,'ADMIN_FLASHCARD_NOT_FOUND');
    await error('/flashcards/999/status','PATCH',{isActive:false},404,'ADMIN_FLASHCARD_NOT_FOUND');
  });
  it('reference changes during mutation map to safe409',async()=>{
    f.fail('createCard',{code:'23503',detail:'private reference SQL'});await error('/flashcards','POST',card,409,'ADMIN_REFERENCE_CONFLICT');
  });
  it('users list supports role/search/pagination and explicitly omits password',async()=>{
    Object.assign(f.users.get(42)!,{password_hash:'private'});f.users.get(42)!.total_xp=100;
    const p=await good('/users?role=USER&search=USER42%40EXAMPLE.COM');assert.equal(p.total,1);assert.equal(p.items[0].level,2);
    assert.equal(p.items[0].role,'USER');assert.equal(p.items[0].lastLogin,null);assert.doesNotMatch(JSON.stringify(p),/password_hash|private/);
    assert.equal((await good('/users?page=2&limit=1')).items[0].id,42);
  });
  it('user detail exposes summaries without authentication secrets and handles missing users',async()=>{
    const d=await good('/users/42');assert.equal(d.user.id,42);assert.equal(d.progress.accuracy,null);assert.equal(d.quiz.accuracy,null);
    assert.equal(d.gamification.level,1);assert.doesNotMatch(JSON.stringify(d),/password|token/);
    for(const path of ['/users/999','/users/999/progress','/users/999/quiz-history'])await error(path,'GET',undefined,404,'ADMIN_USER_NOT_FOUND');
    await error('/users/999/role','PATCH',{role:'ADMIN'},404,'ADMIN_USER_NOT_FOUND');
  });
  it('admin reads another user progress/history with pagination and nullable answer/date mappings',async()=>{
    f.progress.push({flashcard_id:1,english:'apple',vietnamese:'táo',topic_code:'FOOD',memory_level:2,review_count:3,correct_count:2,
      wrong_count:1,is_favorite:true,last_review:null,next_review:null});
    f.history.push({id:9,flashcard_id:1,question:'apple',selected_answer:null,correct_answer:'táo',is_correct:false,answered_at:new Date('2026-10-05T01:00:00Z')});
    const p=await good('/users/42/progress?search=apple&limit=1');assert.equal(p.items[0].reviewCount,3);assert.equal(p.items[0].lastReview,null);
    const h=await good('/users/42/quiz-history?limit=1');assert.equal(h.items[0].selectedAnswer,null);assert.equal(h.items[0].correct,false);
    assert.equal((await good('/users/42/quiz-history?page=2&limit=1')).items.length,0);
    assert.ok(!f.calls.some(c=>c.name.startsWith('set')||c.name.startsWith('create')));
    await error('/users/42/progress','POST',{},404,'ROUTE_NOT_FOUND');
  });
  it('quiz stats maps ranges/topic/counts and null accuracy for zero answers',async()=>{
    assert.equal((await good('/stats/quiz?range=all&topicCode=FOOD')).accuracy,null);
    assert.deepEqual(f.calls.find(c=>c.name==='stats')!.args,[{range:'all',topicCode:'FOOD'}]);
    await error('/stats/quiz?topicCode=MISSING','GET',undefined,404,'ADMIN_TOPIC_NOT_FOUND');
    f.stats.totals={total_answers:'4',correct_answers:'3',wrong_answers:'1'};assert.equal((await good('/stats/quiz')).accuracy,75);
  });
  it('leaderboard preserves full ranking ties across pages and excludes private fields',async()=>{
    f.board.push(...[41,42,43].map((id,i)=>({rank:i<2?'1':'3',user_id:id,username:`u${id}`,full_name:null,avatar_url:null,
      total_xp:i<2?250:10,current_streak:1,longest_streak:1,email:'secret'})));
    const p=await good('/leaderboard?page=2&limit=1');assert.equal(p.items[0].rank,1);assert.equal(p.items[0].userId,42);assert.equal(p.items[0].level,3);
    assert.equal(p.totalPages,3);assert.equal((await good('/leaderboard?page=3&limit=1')).items[0].rank,3);
    assert.doesNotMatch(JSON.stringify(p),/email|secret/);
  });
  for(const path of ['/topics/1','/flashcards/1','/users/42'])it(`no DELETE route ${path}`,async()=>{await error(path,'DELETE',undefined,404,'ROUTE_NOT_FOUND');});
  for(const bad of ['0','-1','01','1.5','1e2','abc','','2147483648'])it(`rejects noncanonical page ${JSON.stringify(bad)} on all paged routes`,async()=>{
    for(const path of ['/topics','/flashcards','/users','/users/42/progress','/users/42/quiz-history','/leaderboard'])
      await error(`${path}?page=${encodeURIComponent(bad)}`,'GET',undefined,400,'VALIDATION_ERROR');
  });
  for(const q of ['limit=0','limit=101','limit=01','limit=1.5','limit=1e2','limit=abc','limit=1&limit=2','page=1&page=2','search=a&search=b'])
    it(`rejects invalid/repeated query ${q}`,async()=>{await error('/topics?'+q,'GET',undefined,400,'VALIDATION_ERROR');});
  for(const [path,q] of [['/users','role=SUPER'],['/users','role=USER&role=ADMIN'],['/flashcards','status=true'],['/flashcards','level=C1'],
    ['/dashboard','range=1d'],['/stats/quiz','range=7d&range=all'],['/topics','search='+ 'x'.repeat(201)]])
    it(`rejects whitelist/search violation ${path} ${q!.slice(0,30)}`,async()=>{await error(path+'?'+q,'GET',undefined,400,'VALIDATION_ERROR');});
  for(const bad of ['0','01','-1','1.2','abc','2147483648'])it(`rejects invalid path ID ${bad}`,async()=>{await error('/users/'+bad,'GET',undefined,400,'VALIDATION_ERROR');});
  for(const input of [{...topic,code:'lower'},{...topic,code:'1NUM'},{...topic,name:' '},{...topic,name:'x'.repeat(101)},
    {...topic,imageUrl:'javascript:alert(1)'},{...topic,role:'ADMIN'}])
    it(`rejects topic body ${JSON.stringify(input).slice(0,90)}`,async()=>{await error('/topics','POST',input,400,'VALIDATION_ERROR');});
  for(const input of [{...card,english:' '},{...card,vietnamese:''},{...card,english:'x'.repeat(151)},
    {...card,level:'C1'},{...card,partOfSpeech:'invalid'},{...card,isActive:'true'},{...card,extra:1}])
    it(`rejects card body ${JSON.stringify(input).slice(0,90)}`,async()=>{await error('/flashcards','POST',input,400,'VALIDATION_ERROR');});
  for(const input of [{role:'ROOT'},{role:'admin'},{role:'ADMIN',userId:41},{}])
    it(`rejects role body ${JSON.stringify(input)}`,async()=>{await error('/users/42/role','PATCH',input,400,'VALIDATION_ERROR');});
  for(const input of [{isActive:'false'},{isActive:0},{isActive:null},{isActive:false,extra:1},{}])
    it(`rejects status body ${JSON.stringify(input)}`,async()=>{await error('/flashcards/1/status','PATCH',input,400,'VALIDATION_ERROR');});
  for(const [path,name] of [['/dashboard','dashboard'],['/topics','topics'],['/flashcards','cards'],['/users','users'],
    ['/users/42','user'],['/users/42/progress','progress'],['/users/42/quiz-history','quizHistory'],['/stats/quiz','stats'],['/leaderboard','leaderboard']])
    it(`generic500 for ${name} without private error leakage`,async()=>{f.fail(name!);const body=await error(path!,'GET',undefined,500,'INTERNAL_SERVER_ERROR');
      assert.doesNotMatch(JSON.stringify(body),/SQL|password_hash|JWT_SECRET|stack/);});
});

describe('Milestone 6A role CLI argument validation (script is NOT executed)',()=>{
  it('normalizes exact email and allows only USER/ADMIN',()=>{
    assert.deepEqual(parseRoleArguments(['--email',' User@Example.com ','--role','ADMIN']),{email:'user@example.com',role:'ADMIN'});
    assert.equal(parseRoleArguments(['--email','u@example.com','--role','USER']).role,'USER');
  });
  for(const args of [[],['--email','u@example.com','--role','ROOT'],['--email','bad','--role','ADMIN'],
    ['--email','u@example.com','--role','ADMIN','extra']])it(`rejects malformed CLI arguments ${JSON.stringify(args)}`,()=>{assert.throws(()=>parseRoleArguments(args));});
});
