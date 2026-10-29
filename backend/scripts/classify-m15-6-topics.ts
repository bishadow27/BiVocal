import 'dotenv/config';
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

type TopicDef={code:string;name:string;description:string;group:'SEMANTIC'|'PART_OF_SPEECH'};
type Row={id:number;english:string;vietnamese:string;part_of_speech:string|null;source_topics:string[]};
type Mapping={flashcardId:number;english:string;vietnamese:string;partOfSpeech:string|null;oldTopic:string;newTopic:string;reason:string;confidence:'HIGH'|'MEDIUM'|'FALLBACK'};
const NEW_TOPICS:TopicDef[]=[
 ['NATURE','Nature & Environment','Thiên nhiên, môi trường, cây cối và địa lý','SEMANTIC'],
 ['FINANCE','Finance & Economy','Tiền bạc, ngân hàng, thương mại và kinh tế','SEMANTIC'],
 ['ARTS','Arts & Literature','Nghệ thuật, âm nhạc, văn học và biểu diễn','SEMANTIC'],
 ['SPORTS','Sports & Fitness','Thể thao, vận động và thi đấu','SEMANTIC'],
 ['LAW_GOVERNMENT','Law & Government','Luật pháp, chính quyền và chính trị','SEMANTIC'],
 ['COMMUNICATION','Communication & Media','Giao tiếp, truyền thông và báo chí','SEMANTIC'],
 ['TIME_CALENDAR','Time & Calendar','Thời gian, lịch và tần suất','SEMANTIC'],
 ['CLOTHING','Clothing & Fashion','Quần áo, phụ kiện và thời trang','SEMANTIC'],
 ['BUILDINGS_PLACES','Buildings & Places','Công trình, địa điểm và không gian công cộng','SEMANTIC'],
 ['LANGUAGE','Language & Grammar','Ngôn ngữ, ngữ pháp và hoạt động dùng từ','SEMANTIC'],
 ['POS_NOUN','Noun','Danh từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_VERB','Verb','Động từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_ADJECTIVE','Adjective','Tính từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_ADVERB','Adverb','Trạng từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_PREPOSITION','Preposition','Giới từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_CONJUNCTION','Conjunction','Liên từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_PRONOUN','Pronoun','Đại từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
 ['POS_PHRASE','Phrase','Cụm từ chưa đủ căn cứ xếp chủ đề ngữ nghĩa','PART_OF_SPEECH'],
].map(([code,name,description,group])=>({code,name,description,group} as TopicDef));
const RULES:Array<{code:string;keys:string[]}>= [
 {code:'FOOD',keys:['food','drink','meal','fruit','vegetable','cook','restaurant','kitchen','thức ăn','đồ uống','nấu','bữa ăn']},
 {code:'HEALTH',keys:['health','body','disease','medical','medicine','hospital','doctor','pain','sức khỏe','bệnh','thuốc','cơ thể']},
 {code:'TRAVEL',keys:['travel','tourism','hotel','airport','journey','holiday','du lịch','khách sạn','sân bay','hành trình']},
 {code:'SCHOOL',keys:['education','school','study','exam','university','student','teacher','giáo dục','trường học','học sinh','giáo viên']},
 {code:'WORK',keys:['work','job','occupation','office','career','employment','công việc','nghề nghiệp','văn phòng']},
 {code:'TECHNOLOGY',keys:['computer','technology','internet','software','digital','machine','máy tính','công nghệ','phần mềm']},
 {code:'ANIMALS',keys:['animal','bird','fish','insect','mammal','động vật','chim','côn trùng']},
 {code:'WEATHER',keys:['weather','climate','rain','snow','wind','storm','thời tiết','khí hậu','mưa','tuyết']},
 {code:'FAMILY',keys:['family','parent','mother','father','sibling','marriage','gia đình','cha','mẹ','anh chị em']},
 {code:'SHOPPING',keys:['shopping','shop','buy','sell','price','market','mua sắm','mua','giá cả']},
 {code:'HOME',keys:['house','home','furniture','room','garden','nhà cửa','nội thất']},
 {code:'TRANSPORTATION',keys:['transport','vehicle','car','bus','train','traffic','xe cộ','giao thông','tàu hỏa']},
 {code:'RELATIONSHIPS',keys:['relationship','friendship','social interaction','quan hệ','tình bạn','giao tiếp xã hội']},
 {code:'EMOTIONS',keys:['emotion','feeling','happy','sad','angry','fear','cảm xúc','vui','buồn','giận']},
 {code:'SCIENCE',keys:['science','physics','chemistry','biology','mathematics','khoa học','vật lý','hóa học','sinh học']},
 {code:'LEISURE',keys:['hobby','leisure','game','entertainment','sở thích','giải trí','trò chơi']},
 {code:'PERSONAL',keys:['personal information','appearance','personality','thông tin cá nhân','ngoại hình','tính cách']},
 {code:'CULTURE',keys:['culture','country','nationality','religion','tradition','văn hóa','quốc gia','tôn giáo','truyền thống']},
 {code:'NATURE',keys:['nature','environment','plant','tree','flower','geography','landscape','ocean','river','thiên nhiên','môi trường','cây','hoa','địa lý']},
 {code:'FINANCE',keys:['finance','money','bank','economy','business','tax','loan','currency','tài chính','tiền','ngân hàng','kinh tế','thuế']},
 {code:'ARTS',keys:['art','music','literature','film','theatre','painting','dance','nghệ thuật','âm nhạc','văn học','điện ảnh']},
 {code:'SPORTS',keys:['sport','football','tennis','exercise','athlete','competition','thể thao','bóng đá','tập luyện','vận động viên']},
 {code:'LAW_GOVERNMENT',keys:['law','legal','government','politics','court','crime','police','luật','chính phủ','chính trị','tòa án','tội phạm']},
 {code:'COMMUNICATION',keys:['communication','media','newspaper','telephone','message','broadcast','truyền thông','báo chí','điện thoại','tin nhắn']},
 {code:'TIME_CALENDAR',keys:['time','calendar','day','month','year','hour','frequency','thời gian','lịch','ngày','tháng','năm','giờ']},
 {code:'CLOTHING',keys:['clothes','clothing','fashion','shirt','shoe','dress','quần áo','thời trang','áo','giày','váy']},
 {code:'BUILDINGS_PLACES',keys:['building','place','city','village','street','road','construction','tòa nhà','địa điểm','thành phố','đường phố']},
 {code:'LANGUAGE',keys:['language','grammar','word','sentence','speak','write','read','ngôn ngữ','ngữ pháp','từ ngữ','đọc','viết']},
];
const POS:Record<string,string>={noun:'POS_NOUN',verb:'POS_VERB',adjective:'POS_ADJECTIVE',adverb:'POS_ADVERB',preposition:'POS_PREPOSITION',conjunction:'POS_CONJUNCTION',pronoun:'POS_PRONOUN',phrase:'POS_PHRASE'};
// Keep Vietnamese diacritics: removing them makes short words such as "cá" match "cả/ca".
const normalize=(value:string)=>value.normalize('NFC').toLocaleLowerCase('vi-VN');
function containsTerm(haystack:string,key:string):boolean{
 const term=normalize(key).trim(),tokens=new Set(haystack.split(/[^\p{L}\p{N}]+/u).filter(Boolean));
 if(!term.includes(' '))return tokens.has(term);
 return (` ${haystack.replace(/[^\p{L}\p{N}]+/gu,' ').replace(/\s+/g,' ').trim()} `).includes(` ${term} `);
}
function classify(row:Row):Mapping{
 const lexical=normalize(`${row.english} ${row.vietnamese}`),metadata=normalize(row.source_topics.join(' '));
 for(const rule of RULES){const sourceHit=rule.keys.find(key=>containsTerm(metadata,key));if(sourceHit)return {flashcardId:row.id,english:row.english,vietnamese:row.vietnamese,partOfSpeech:row.part_of_speech,oldTopic:'GENERAL_VOCABULARY',newTopic:rule.code,reason:`SOURCE_TOPIC:${sourceHit}`,confidence:'HIGH'};}
 for(const rule of RULES){const hit=rule.keys.find(key=>containsTerm(lexical,key));if(hit)return {flashcardId:row.id,english:row.english,vietnamese:row.vietnamese,partOfSpeech:row.part_of_speech,oldTopic:'GENERAL_VOCABULARY',newTopic:rule.code,reason:`SENSE_KEYWORD:${hit}`,confidence:'MEDIUM'};}
 const target=POS[row.part_of_speech??'']??'POS_PHRASE';return {flashcardId:row.id,english:row.english,vietnamese:row.vietnamese,partOfSpeech:row.part_of_speech,oldTopic:'GENERAL_VOCABULARY',newTopic:target,reason:`POS_FALLBACK:${row.part_of_speech??'unknown'}`,confidence:'FALLBACK'};
}
const args=process.argv.slice(2),apply=args.includes('--apply'),confirm=args[args.indexOf('--database-confirm')+1];
const env=readDatabaseEnv(process.env);if(apply&&(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)||confirm!==env.DB_NAME))throw new Error('Apply requires local DB and exact confirmation');
const planUrl=new URL('../.m15-6-verification/topic-mapping.jsonl',import.meta.url),csvUrl=new URL('../.m15-6-verification/topic-mapping.csv',import.meta.url),summaryUrl=new URL('../.m15-6-verification/topic-mapping-summary.json',import.meta.url);
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:2});
try{
 if(!apply){const rows=(await pool.query<Row>(`SELECT f.id,f.english,f.vietnamese,f.part_of_speech,
   COALESCE(ARRAY(SELECT jsonb_array_elements_text(COALESCE(c.original_payload->'sourceTopics','[]'::jsonb))),'{}') source_topics
   FROM flashcards f JOIN topics t ON t.id=f.topic_id LEFT JOIN vocabulary_review_candidates c ON c.published_flashcard_id=f.id
   WHERE t.code='GENERAL_VOCABULARY' ORDER BY f.id`)).rows;if(rows.length!==14329)throw new Error(`Expected 14329 GENERAL cards, got ${rows.length}`);
   const mappings=rows.map(classify),text=mappings.map(value=>JSON.stringify(value)).join('\n')+'\n';await mkdir(fileURLToPath(new URL('../.m15-6-verification/',import.meta.url)),{recursive:true});await writeFile(planUrl,text);
   const quote=(v:unknown)=>`"${String(v??'').replaceAll('"','""')}"`;await writeFile(csvUrl,['flashcard_id,english,vietnamese,pos,old_topic,new_topic,reason,confidence',...mappings.map(m=>[m.flashcardId,m.english,m.vietnamese,m.partOfSpeech,m.oldTopic,m.newTopic,m.reason,m.confidence].map(quote).join(','))].join('\n')+'\n');
   const distribution=Object.fromEntries([...new Set(mappings.map(m=>m.newTopic))].sort().map(code=>[code,mappings.filter(m=>m.newTopic===code).length]));const semantic=mappings.filter(m=>!m.newTopic.startsWith('POS_')).length;
   const summary={database:env.DB_NAME,total:mappings.length,semantic,posFallback:mappings.length-semantic,unclassified:0,distribution,sha256:createHash('sha256').update(text).digest('hex')};await writeFile(summaryUrl,JSON.stringify(summary,null,2)+'\n');console.log(JSON.stringify(summary,null,2));process.exit(0);
 }
 const text=await readFile(planUrl,'utf8'),mappings=text.split(/\r?\n/).filter(Boolean).map(line=>JSON.parse(line) as Mapping);if(mappings.length!==14329)throw new Error('Frozen mapping count mismatch');
 const current=(await pool.query<{general:number;general_links:number;total:number}>(`SELECT (SELECT count(*)::int FROM flashcards) total,
   (SELECT count(*)::int FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.code='GENERAL_VOCABULARY') general,
   (SELECT count(*)::int FROM flashcard_topics ft JOIN topics t ON t.id=ft.topic_id WHERE t.code='GENERAL_VOCABULARY') general_links`)).rows[0]!;
 if(current.total!==20000||![0,14329].includes(current.general))throw new Error(`Unexpected database state ${JSON.stringify(current)}`);
 if(current.general===0&&current.general_links===0){console.log(JSON.stringify({database:env.DB_NAME,insertedTopics:0,moved:0,idempotent:true}));process.exit(0);}
 const client=await pool.connect();try{await client.query('BEGIN');for(const topic of NEW_TOPICS)await client.query(`INSERT INTO topics(code,name,description,image_url) VALUES($1,$2,$3,NULL) ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description`,[topic.code,topic.name,topic.description]);
   for(let offset=0;offset<mappings.length;offset+=500){const batch=mappings.slice(offset,offset+500);await client.query(`WITH input AS (SELECT (v->>'flashcardId')::int id,v->>'newTopic' code FROM jsonb_array_elements($1::jsonb)v), targets AS (SELECT i.id,t.id topic_id FROM input i JOIN topics t ON t.code=i.code), old AS (SELECT f.id FROM flashcards f JOIN topics t ON t.id=f.topic_id JOIN targets x ON x.id=f.id WHERE t.code='GENERAL_VOCABULARY'), cleared AS (UPDATE flashcard_topics ft SET is_primary=FALSE FROM old WHERE ft.flashcard_id=old.id RETURNING ft.flashcard_id), linked AS (INSERT INTO flashcard_topics(flashcard_id,topic_id,is_primary) SELECT x.id,x.topic_id,TRUE FROM targets x JOIN (SELECT DISTINCT flashcard_id FROM cleared)c ON c.flashcard_id=x.id ON CONFLICT(flashcard_id,topic_id) DO UPDATE SET is_primary=TRUE RETURNING flashcard_id) UPDATE flashcards f SET topic_id=x.topic_id,updated_at=CURRENT_TIMESTAMP FROM targets x JOIN linked l ON l.flashcard_id=x.id WHERE f.id=x.id`,[JSON.stringify(batch)]);
     await client.query(`DELETE FROM flashcard_topics ft USING topics t WHERE ft.topic_id=t.id AND t.code='GENERAL_VOCABULARY' AND ft.flashcard_id=ANY($1::int[])`,[batch.map(item=>item.flashcardId)]);
   }
   const remaining=(await client.query<{count:number}>(`SELECT count(*)::int count FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.code='GENERAL_VOCABULARY'`)).rows[0]!.count;if(remaining!==0)throw new Error(`GENERAL remaining ${remaining}`);await client.query('COMMIT');
 }catch(error){await client.query('ROLLBACK');throw error;}finally{client.release();}
 console.log(JSON.stringify({database:env.DB_NAME,moved:mappings.length,newTopics:NEW_TOPICS.length,planSha256:createHash('sha256').update(text).digest('hex'),status:'APPLIED'},null,2));
}finally{await pool.end();}
