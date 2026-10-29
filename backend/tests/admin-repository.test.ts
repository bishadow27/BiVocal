import assert from 'node:assert/strict';
import { describe,it } from 'node:test';
import type { Pool } from 'pg';
import { createAdminRepository } from '../src/modules/admin/admin.repository.js';
import { adminCount } from '../src/modules/admin/admin.service.js';

function fixture(){
  const calls:{sql:string;params:unknown[]}[]=[];
  const pool={async query(sql:string,params:unknown[]=[]){calls.push({sql,params});
    if(sql.includes('COUNT(*)::text AS total FROM'))return{rows:[{total:'0'}]};
    if(sql.startsWith('SELECT')&&sql.includes('AS total_answers'))return{rows:[{total_answers:'0',correct_answers:'0',wrong_answers:'0'}]};
    return{rows:[]};}} as unknown as Pool;
  return{repository:createAdminRepository(pool),calls};
}
describe('M6A parameterized repository contracts (SQL not executed)',()=>{
  it('search is parameterized literal substring, escapes LIKE wildcards, and stable pagination offset',async()=>{
    const f=fixture();const attack="%' OR 1=1 --_\\";
    await f.repository.topics({search:attack,page:3,limit:10});
    assert.equal(f.calls.length,2);for(const c of f.calls){assert.ok(!c.sql.includes(attack));assert.equal(c.params[0],"%\\%' OR 1=1 --\\_\\\\%");}
    assert.deepEqual(f.calls[1]!.params.slice(1),[10,20]);assert.match(f.calls[1]!.sql,/ORDER BY t.id ASC/);
  });
  it('card filters only use parameters and fixed SQL fragments',async()=>{
    const f=fixture();await f.repository.cards({search:'apple',topicCode:'FOOD',status:'INACTIVE',level:'A1',page:1,limit:100});
    assert.deepEqual(f.calls[1]!.params,['%apple%','FOOD','INACTIVE','A1',100,0]);
    assert.match(f.calls[1]!.sql,/f.is_active IS NOT TRUE/);assert.doesNotMatch(f.calls[1]!.sql,/SELECT \*/);
  });
  it('user filters and exact CLI email update never select password or trust role from a JWT',async()=>{
    const f=fixture();await f.repository.users({search:'user',role:'ADMIN',page:1,limit:20});
    await f.repository.account(42);await f.repository.setRoleByEmail('exact@example.com','USER');
    for(const c of f.calls)assert.doesNotMatch(c.sql,/password_hash|SELECT \*/);
    assert.deepEqual(f.calls.at(-1)!.params,['exact@example.com','USER']);assert.match(f.calls.at(-1)!.sql,/WHERE LOWER\(email\)=\$1/);
    assert.match(f.calls[2]!.sql,/u.role/);
  });
  it('rank window excludes tie-break ID and precedes LIMIT/OFFSET, zero XP excluded',async()=>{
    const f=fixture();await f.repository.leaderboard({page:2,limit:5});const q=f.calls[1]!;
    assert.match(q.sql,/RANK\(\) OVER \(ORDER BY s.total_xp DESC\)/);assert.match(q.sql,/WHERE s.total_xp>0/);
    assert.match(q.sql,/ORDER BY s.total_xp DESC,s.user_id ASC LIMIT/);assert.deepEqual(q.params,[5,5]);
    assert.doesNotMatch(q.sql,/email|password|last_login/);
  });
  it('stats range maps to numeric constants, topic parameter and UTC zero-filled calendar',async()=>{
    for(const [range,days] of [['7d',7],['30d',30],['all',null]] as const){const f=fixture();await f.repository.stats({range,topicCode:'FOOD'});
      assert.equal(f.calls.length,3);assert.ok(f.calls.every(c=>c.params[0]===days&&c.params[1]==='FOOD'));
      assert.ok(f.calls.every(c=>!c.sql.includes('FOOD')));assert.match(f.calls[2]!.sql,/generate_series\(0,\$1::integer-1\)/);
      assert.match(f.calls[2]!.sql,/AT TIME ZONE 'UTC'/);}
  });
  it('admin progress includes inactive history while summaries reuse existing active-only rule',async()=>{
    const f=fixture();await f.repository.progress(42,{search:'',page:1,limit:20});
    assert.ok(f.calls.every(c=>!c.sql.includes('is_active')));assert.deepEqual(f.calls[1]!.params,[42,'%%',20,0]);
  });
  it('PUT card does not update status; dedicated status SQL does not delete history',async()=>{
    const f=fixture();await f.repository.updateCard(1,{topicCode:'FOOD',english:'x',vietnamese:'y',pronunciation:null,partOfSpeech:null,example:null,exampleVi:null,level:'A1'});
    assert.doesNotMatch(f.calls[0]!.sql,/SET is_active|is_active=|DELETE|TRUNCATE/);
    await f.repository.setCardStatus(1,false);assert.match(f.calls[1]!.sql,/SET is_active=\$2/);assert.doesNotMatch(f.calls[1]!.sql,/DELETE|TRUNCATE/);
  });
  it('unsafe aggregate conversion fails instead of rounding large bigint counts silently',()=>{
    assert.equal(adminCount('0'),0);assert.equal(adminCount('123'),123);
    for(const bad of ['-1','NaN','9007199254740992','1.5'])assert.throws(()=>adminCount(bad));
  });
});
