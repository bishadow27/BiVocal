import type { AdminRepository } from '../../src/modules/admin/admin.repository.js';
import type { AdminUserRow, CardRow, TopicRow } from '../../src/modules/admin/admin.types.js';
import type { Pagination } from '../../src/modules/admin/admin.schema.js';

// Functional repository double for HTTP contracts; does not execute PostgreSQL.
export function adminDouble() {
  const now=new Date('2026-10-05T00:00:00Z');
  const users=new Map<number,AdminUserRow>([41,42].map((id)=>[id,{id,username:`user${id}`,email:`user${id}@example.com`,
    full_name:`User ${id}`,avatar_url:null,role:id===41?'ADMIN':'USER',created_at:now,last_login:null,
    total_xp:0,current_streak:0,longest_streak:0}]));
  const topics=new Map<number,TopicRow>([[1,{id:1,code:'FOOD',name:'Food',description:null,image_url:null,flashcard_count:'1'}]]);
  const cards=new Map<number,CardRow>([[1,{id:1,topic_id:1,topic_code:'FOOD',topic_name:'Food',english:'apple',vietnamese:'táo',
    pronunciation:null,part_of_speech:'noun',example:null,example_vi:null,level:'A1',is_active:true,created_at:now,updated_at:now}]]);
  const calls: {name:string;args:unknown[]}[]=[]; let fail: string|undefined; let dbError: unknown;
  let nextTopic=2,nextCard=2;
  const record=(name:string,...args:unknown[])=>{calls.push({name,args});if(fail===name) throw dbError??new Error('private SQL password_hash JWT_SECRET');};
  const paginate=<T>(rows:T[],q:Pagination)=>({rows:rows.slice((q.page-1)*q.limit,q.page*q.limit),total:String(rows.length)});
  const contains=(value:string|null,search:string)=>value?.toLowerCase().includes(search.toLowerCase())??false;
  const stats: Awaited<ReturnType<AdminRepository['stats']>>={totals:{total_answers:'0',correct_answers:'0',wrong_answers:'0'},byDay:[],byTopic:[]};
  const board: Awaited<ReturnType<AdminRepository['leaderboard']>>['rows']=[];
  const progress: Awaited<ReturnType<AdminRepository['progress']>>['rows']=[];
  const history: Awaited<ReturnType<AdminRepository['quizHistory']>>['rows']=[];
  const repository:AdminRepository={
    async account(id){record('account',id);return users.get(id);},
    async setRole(id,role){record('setRole',id,role);const u=users.get(id);if(u)u.role=role;return u;},
    async setRoleByEmail(email,role){record('setRoleByEmail',email,role);const u=[...users.values()].find(u=>u.email===email);if(u)u.role=role;return u;},
    async dashboard(range){record('dashboard',range);return{counts:{users:String(users.size),cards:String(cards.size),
      active:String([...cards.values()].filter(c=>c.is_active).length),inactive:String([...cards.values()].filter(c=>!c.is_active).length),
      xp:String([...users.values()].reduce((sum,u)=>sum+u.total_xp,0))},quiz:stats,topUsers:board.slice(0,5)};},
    async stats(q){record('stats',q);return stats;},
    async topics(q){record('topics',q);return paginate([...topics.values()].filter(t=>contains(t.code,q.search)||contains(t.name,q.search)),q);},
    async topicByCode(code){record('topicByCode',code);return[...topics.values()].find(t=>t.code===code);},
    async createTopic(input){record('createTopic',input);if([...topics.values()].some(t=>t.code===input.code))throw{code:'23505',constraint:'topics_code_key'};
      const t={id:nextTopic++,code:input.code,name:input.name,description:input.description,image_url:input.imageUrl,flashcard_count:'0'};topics.set(t.id,t);return t;},
    async updateTopic(id,input){record('updateTopic',id,input);const t=topics.get(id);if(!t)return undefined;
      Object.assign(t,{code:input.code,name:input.name,description:input.description,image_url:input.imageUrl});return t;},
    async cards(q){record('cards',q);return paginate([...cards.values()].filter(c=>(contains(c.english,q.search)||contains(c.vietnamese,q.search))&&
      (!q.topicCode||c.topic_code===q.topicCode)&&(!q.level||c.level===q.level)&&(q.status==='ALL'||c.is_active===(q.status==='ACTIVE'))),q);},
    async createCard(input){record('createCard',input);const t=[...topics.values()].find(t=>t.code===input.topicCode);if(!t)return undefined;
      if([...cards.values()].some(c=>c.english.toLowerCase()===input.english.toLowerCase()&&c.vietnamese.toLowerCase()===input.vietnamese.toLowerCase()))
        throw{code:'23505',constraint:'idx_flashcards_unique_word_meaning'};
      const c:CardRow={id:nextCard++,topic_id:t.id,topic_code:t.code,topic_name:t.name,english:input.english,vietnamese:input.vietnamese,
        pronunciation:input.pronunciation,part_of_speech:input.partOfSpeech,example:input.example,example_vi:input.exampleVi,
        level:input.level,is_active:input.isActive,created_at:now,updated_at:now};cards.set(c.id,c);return c;},
    async updateCard(id,input){record('updateCard',id,input);const c=cards.get(id),t=[...topics.values()].find(t=>t.code===input.topicCode);
      if(!c||!t)return undefined;Object.assign(c,{topic_id:t.id,topic_code:t.code,topic_name:t.name,english:input.english,vietnamese:input.vietnamese,
        pronunciation:input.pronunciation,part_of_speech:input.partOfSpeech,example:input.example,example_vi:input.exampleVi,level:input.level});return c;},
    async setCardStatus(id,active){record('setCardStatus',id,active);const c=cards.get(id);if(c)c.is_active=active;return c;},
    async users(q){record('users',q);return paginate([...users.values()].filter(u=>(q.role==='ALL'||u.role===q.role)&&
      (contains(u.username,q.search)||contains(u.email,q.search)||contains(u.full_name,q.search))),q);},
    async user(id){record('user',id);return users.get(id);},
    async progressSummary(id){record('progressSummary',id);return{tracked_cards:'0',reviewed_cards:'0',favorite_cards:'0',mastered_cards:'0',
      total_reviews:'0',correct_answers:'0',wrong_answers:'0'};},
    async quizSummary(id){record('quizSummary',id);return stats.totals;},
    async score(id){record('score',id);const u=users.get(id);return u&&{total_xp:u.total_xp,current_streak:u.current_streak,longest_streak:u.longest_streak,last_activity_date:null};},
    async progress(id,q){record('progress',id,q);return paginate(progress,q);},
    async quizHistory(id,q){record('quizHistory',id,q);return paginate(history,q);},
    async leaderboard(q){record('leaderboard',q);return paginate(board,q);},
  };
  return{repository,users,topics,cards,calls,stats,board,progress,history,
    fail(name:string,error?:unknown){fail=name;dbError=error;}};
}
