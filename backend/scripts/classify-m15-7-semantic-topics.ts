import 'dotenv/config';
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

type Row={id:number;english:string;vietnamese:string;part_of_speech:string|null;old_topic:string;source_topics:string[]};
type Mapping={flashcardId:number;english:string;vietnamese:string;partOfSpeech:string|null;oldTopic:string;newTopic:string;reason:string;confidence:'HIGH'|'MEDIUM';evidence:string[]};
type TopicRule={code:string;heads:string[];sense:string[];exclude?:string[]};
type Learned={topic:string;purity:number;count:number;weight:number};

const POS_TOPICS=['POS_NOUN','POS_VERB','POS_ADJECTIVE','POS_ADVERB','POS_PREPOSITION','POS_CONJUNCTION','POS_PRONOUN','POS_PHRASE'];
const RULES:TopicRule[]=[
 {code:'ANIMALS',heads:['ant','ape','bear','bee','bird','butterfly','camel','cat','cattle','chicken','cow','crab','deer','dog','dolphin','donkey','duck','eagle','elephant','fish','fox','frog','goat','goose','hawk','horse','insect','lion','lizard','monkey','mosquito','mouse','octopus','owl','panda','parrot','penguin','pig','rabbit','rat','shark','sheep','snake','spider','tiger','turtle','whale','wolf','worm'],sense:['động vật','con vật','loài chim','loài cá','côn trùng','gia súc','động vật có vú']},
 {code:'ARTS',heads:['actor','actress','ballet','canvas','cinema','composer','concert','drama','film','gallery','guitar','literature','melody','museum','novel','opera','orchestra','paintbrush','painter','painting','piano','poem','poet','sculptor','sculpture','theatre','violin'],sense:['nghệ thuật','âm nhạc','văn học','hội họa','điêu khắc','sân khấu','điện ảnh','nhạc cụ']},
 {code:'BUILDINGS_PLACES',heads:['apartment','bridge','building','castle','cathedral','church','factory','garage','harbour','library','mosque','palace','park','port','prison','restaurant','stadium','station','temple','theater','tower','warehouse'],sense:['tòa nhà','công trình kiến trúc','địa điểm công cộng','nhà ga','nhà kho']},
 {code:'CLOTHING',heads:['belt','blouse','boot','bracelet','button','cap','coat','collar','costume','dress','earring','garment','glove','handbag','hat','jacket','jeans','necklace','pocket','scarf','shirt','shoe','shorts','skirt','sock','suit','sweater','tie','trousers','uniform','wallet'],sense:['quần áo','trang phục','thời trang','giày dép']},
 {code:'COMMUNICATION',heads:['advertisement','broadcast','email','internet','journalism','magazine','mail','media','message','newspaper','press','radio','telephone','television'],sense:['truyền thông','báo chí','phát thanh','thư điện tử','tin nhắn']},
 {code:'CULTURE',heads:['ceremony','custom','festival','heritage','myth','religion','ritual','tradition'],sense:['văn hóa','phong tục','truyền thống','tôn giáo','lễ hội','di sản']},
 {code:'DAILY_LIFE',heads:['appointment','bath','breakfast','cleaning','dinner','lunch','routine','shower'],sense:['sinh hoạt hằng ngày','hàng ngày','thường nhật','dịch vụ công cộng']},
 {code:'EMOTIONS',heads:['anger','anxiety','delight','emotion','fear','grief','happiness','hatred','joy','love','mood','sadness','sorrow'],sense:['cảm xúc','tâm trạng','vui mừng','buồn bã','tức giận','lo lắng','sợ hãi']},
 {code:'FAMILY',heads:['aunt','brother','child','cousin','daughter','family','father','grandfather','grandmother','husband','mother','nephew','niece','parent','sister','son','uncle','wife'],sense:['gia đình','cha mẹ','anh chị em','họ hàng','vợ chồng']},
 {code:'FINANCE',heads:['accounting','bank','banking','budget','cash','coin','currency','debt','deposit','dividend','economy','finance','insurance','interest','investment','loan','mortgage','profit','salary','tax','wage'],sense:['tài chính','tiền tệ','ngân hàng','khoản vay','lãi suất','thuế','tiền lương','ngân sách']},
 {code:'FOOD',heads:['apple','banana','beef','bread','butter','cake','cheese','coffee','cook','cooking','egg','flour','food','fruit','juice','meat','milk','onion','potato','rice','salt','soup','sugar','tea','vegetable','wine'],sense:['thức ăn','đồ ăn','đồ uống','nấu ăn','rau quả','món ăn','gia vị']},
 {code:'HEALTH',heads:['ambulance','anatomy','antibiotic','cancer','clinic','dentist','disease','doctor','fever','health','hospital','illness','medicine','nurse','patient','pharmacy','surgery','symptom','therapy','virus'],sense:['sức khỏe','y học','bệnh tật','bệnh viện','thuốc chữa','cơ thể người','triệu chứng','điều trị']},
 {code:'HOME',heads:['armchair','bathroom','bed','bedroom','blanket','carpet','ceiling','chair','cupboard','curtain','desk','door','furniture','garden','house','kitchen','lamp','refrigerator','roof','sofa','stove','table','wardrobe','window'],sense:['đồ gia dụng','nội thất','nhà ở','phòng ngủ','phòng khách','nhà bếp','tủ lạnh']},
 {code:'LANGUAGE',heads:['adjective','adverb','alphabet','dictionary','grammar','language','letter','noun','phrase','pronoun','sentence','syllable','translation','verb','vocabulary'],sense:['ngôn ngữ','ngữ pháp','từ vựng','câu văn','dịch thuật']},
 {code:'LAW_GOVERNMENT',heads:['arrest','constitution','court','crime','criminal','democracy','election','government','judge','jury','law','lawyer','minister','parliament','police','politics','president','prison','senate','vote'],sense:['luật pháp','chính phủ','chính trị','tòa án','cảnh sát','tội phạm','bầu cử']},
 {code:'LEISURE',heads:['chess','entertainment','game','hobby','holiday','picnic','puzzle','toy'],sense:['giải trí','sở thích','trò chơi','thời gian rảnh']},
 {code:'NATURE',heads:['beach','canyon','cave','cliff','coast','desert','earthquake','environment','forest','island','jungle','lake','landscape','mountain','nature','ocean','plant','river','sea','soil','tree','valley','volcano','waterfall'],sense:['thiên nhiên','môi trường','địa lý','động đất','núi lửa','sông ngòi','rừng cây','đại dương']},
 {code:'PERSONAL',heads:['address','age','appearance','birthday','character','identity','name','personality','surname'],sense:['thông tin cá nhân','ngoại hình','tính cách','họ tên']},
 {code:'RELATIONSHIPS',heads:['colleague','friend','friendship','neighbour','partner','relationship'],sense:['mối quan hệ','tình bạn','quan hệ xã hội','đồng nghiệp']},
 {code:'SCHOOL',heads:['classroom','college','education','exam','homework','lesson','pupil','school','student','study','teacher','textbook','university'],sense:['giáo dục','trường học','học sinh','giáo viên','kỳ thi']},
 {code:'SCIENCE',heads:['astronomy','biology','chemistry','experiment','laboratory','mathematics','physics','research','science','scientist'],sense:['khoa học','toán học','vật lý','hóa học','sinh học','thí nghiệm']},
 {code:'SHOPPING',heads:['basket','customer','discount','market','price','purchase','receipt','retail','sale','shop','shopping','store','supermarket'],sense:['mua sắm','cửa hàng bán','khách hàng','giá bán','hóa đơn mua hàng']},
 {code:'SPORTS',heads:['athlete','athletics','basketball','boxing','championship','coach','cricket','cycling','football','golf','gym','hockey','match','race','rugby','soccer','sport','stadium','swimming','tennis','tournament','volleyball'],sense:['thể thao','vận động viên','thi đấu','bóng đá','quần vợt','tập thể dục']},
 {code:'TECHNOLOGY',heads:['algorithm','application','computer','database','device','digital','hardware','internet','keyboard','laptop','network','printer','robot','software','technology','website'],sense:['công nghệ','máy tính','phần mềm','phần cứng','mạng internet','thiết bị điện tử']},
 {code:'TIME_CALENDAR',heads:['afternoon','calendar','century','decade','evening','hour','midnight','minute','month','morning','second','today','tomorrow','week','weekend','year','yesterday'],sense:['thời gian','lịch','giờ phút','ngày tháng','tuần lễ']},
 {code:'TRANSPORTATION',heads:['aircraft','bicycle','boat','bus','car','driver','lorry','motorcycle','railway','ship','taxi','traffic','train','transport','truck','vehicle'],sense:['giao thông','phương tiện giao thông','xe cộ','tàu hỏa','đường sắt']},
 {code:'TRAVEL',heads:['airport','baggage','flight','hotel','journey','luggage','passport','reservation','suitcase','ticket','tourism','tourist','travel','trip','visa'],sense:['du lịch','hành trình','khách sạn','sân bay','hộ chiếu','hành lý']},
 {code:'WEATHER',heads:['climate','cloud','fog','forecast','frost','hurricane','rain','rainfall','snow','storm','sunshine','temperature','thunder','weather','wind'],sense:['thời tiết','khí hậu','mưa bão','nhiệt độ','dự báo thời tiết']},
 {code:'WORK',heads:['career','company','employee','employer','employment','factory','industry','interview','job','manager','office','profession','retire','staff','work','worker','workplace'],sense:['công việc','nghề nghiệp','nhân viên','văn phòng','nơi làm việc']},
];

// Only these headwords are sufficiently domain-specific without relying on an ambiguous sense.
// Other RULES.heads are supporting vocabulary only and require an exact Vietnamese/source phrase.
const SAFE_HEADS:Record<string,Set<string>>=Object.fromEntries(Object.entries({
 ANIMALS:['butterfly','elephant','mosquito','octopus','penguin'],
 ARTS:['ballet','composer','concert','literature','melody','opera','orchestra','painter','painting','piano','poem','poet','sculptor','sculpture','violin'],
 BUILDINGS_PLACES:['apartment','building','cathedral','mosque','palace','temple','tower','warehouse'],
 CLOTHING:['blouse','costume','earring','garment','handbag','jacket','jeans','necklace','scarf','shirt','shorts','skirt','sweater','trousers','uniform'],
 COMMUNICATION:['broadcast','email','journalism','magazine','media','newspaper','radio','telephone','television'],
 CULTURE:['ceremony','festival','heritage','myth','religion','ritual','tradition'],
 DAILY_LIFE:['routine','shower'],
 EMOTIONS:['anxiety','antipathy','delight','emotion','empathy','grief','happiness','hatred','joy','mood','sadness','sorrow','sympathy'],
 FAMILY:['aunt','brother','cousin','daughter','family','father','grandfather','grandmother','mother','nephew','niece','parent','sister','son','uncle'],
 FINANCE:['accounting','banking','budget','cash','currency','debt','economy','finance','insurance','investment','loan','mortgage','profit','salary','tax','wage'],
 FOOD:['banana','cheese','coffee','flour','juice','onion','potato','rice','vegetable'],
 HEALTH:['ambulance','anatomy','antibiotic','cancer','clinic','dentist','disease','doctor','hospital','illness','medicine','pharmacy','physician','surgery','symptom','therapy','virus'],
 HOME:['armchair','bathroom','bedroom','furniture','refrigerator','sofa','stove','wardrobe'],
 LANGUAGE:['alphabet','dictionary','grammar','language','lexicology','noun','pronoun','syllable','vocabulary'],
 LAW_GOVERNMENT:['democracy','election','government','lawyer','parliament','police','politics','senate'],
 LEISURE:['chess','hobby','puzzle','toy'],
 NATURE:['canyon','earthquake','environment','forest','jungle','mountain','nature','ocean','river','valley','volcano','waterfall'],
 PERSONAL:['birthday','personality','surname'],
 RELATIONSHIPS:['colleague','coworker','friendship','relationship'],
 SCHOOL:['classroom','college','education','homework','pupil','school','student','teacher','textbook','university'],
 SCIENCE:['astronomy','biology','chemistry','laboratory','mathematics','physics','science','scientist'],
 SHOPPING:['retail','shopping','supermarket'],
 SPORTS:['athlete','athletics','basketball','boxing','championship','cricket','football','golf','hockey','rugby','soccer','sport','tennis','tournament','volleyball'],
 TECHNOLOGY:['algorithm','database','hardware','internet','keyboard','laptop','robot','software','technology','website'],
 TIME_CALENDAR:['calendar','century','decade','midnight','today','tomorrow','weekend','yesterday'],
 TRANSPORTATION:['aircraft','bicycle','lorry','motorcycle','railway','taxi','traffic','truck','vehicle'],
 TRAVEL:['airport','baggage','hotel','luggage','passport','suitcase','tourism','tourist','visa'],
 WEATHER:['climate','fog','frost','hurricane','rainfall','sunshine','weather'],
 WORK:['career','employee','employer','employment','profession','workplace'],
}).map(([code,items])=>[code,new Set(items)]));

const SIGNALS:Record<string,{strong:string[];weak:string[];exclude?:string[]}>= {
 ANIMALS:{strong:['động vật','côn trùng','gia súc','gia cầm','loài chim','loài cá'],weak:['con','chim','thú','cá','bọ','trứng','đuôi','cánh'],exclude:['cá nhân','cá biệt','chim mồi chính trị']},
 ARTS:{strong:['nghệ thuật','âm nhạc','văn học','hội họa','điêu khắc','sân khấu','điện ảnh','nhạc cụ','tiểu thuyết','khiêu vũ','nhạc','thơ','kịch','phim'],weak:['tranh','họa','múa','diễn','tác phẩm','nghệ sĩ'],exclude:['ngây thơ','thơ ấu','thơ thẩn','lơ thơ','kịch liệt','nguy kịch']},
 BUILDINGS_PLACES:{strong:['tòa nhà','công trình kiến trúc','nhà thờ','lâu đài','nhà ga','nhà kho','địa điểm công cộng'],weak:['kiến trúc','xây dựng','cầu','cảng','tháp','đền','chùa']},
 CLOTHING:{strong:['quần áo','trang phục','thời trang','giày dép','vải','mặc','giày'],weak:['áo','quần','mũ','váy','đeo','may'],exclude:['mặc dù','dẫu cho','bỏ mặc','phó mặc','mặc cả']},
 COMMUNICATION:{strong:['truyền thông','báo chí','phát thanh','truyền hình','thư điện tử','tin nhắn'],weak:['báo','tạp chí','điện thoại','thư','phát sóng']},
 CULTURE:{strong:['văn hóa','phong tục','truyền thống','tôn giáo','lễ hội','nghi lễ','di sản'],weak:['dân tộc','tín ngưỡng','thần thoại']},
 DAILY_LIFE:{strong:['sinh hoạt hằng ngày','sinh hoạt hàng ngày','thường nhật','dịch vụ công cộng'],weak:['tắm','ngủ','thức dậy','dọn dẹp','ăn sáng']},
 EMOTIONS:{strong:['cảm xúc','tâm trạng','vui mừng','buồn bã','tức giận','lo lắng','sợ hãi','đau khổ'],weak:['vui','buồn','giận','lo','sợ','yêu','ghét','thất vọng','hạnh phúc']},
 FAMILY:{strong:['gia đình','cha mẹ','anh chị em','họ hàng','vợ chồng'],weak:['cha','mẹ','bố','con trai','con gái','ông','bà','cô','dì','chú','bác','vợ','chồng']},
 FINANCE:{strong:['tài chính','tiền tệ','ngân hàng','khoản vay','lãi suất','tiền lương','ngân sách','thị trường chứng khoán','nợ'],weak:['tiền','vốn','lãi','đầu tư','bảo hiểm','thuế','lương','kinh tế','cổ phiếu']},
 FOOD:{strong:['thức ăn','đồ ăn','đồ uống','nấu ăn','món ăn','gia vị','thực phẩm'],weak:['ăn','uống','bánh','thịt','rau','quả','sữa','nấu','chiên','rang','luộc','ngọt','mặn'],exclude:['xương bánh chè','đầu gối','ăn thịt người','ngộ độc','bắp thịt']},
 HEALTH:{strong:['sức khỏe','y học','y tế','bệnh tật','bệnh viện','thuốc chữa','cơ thể người','triệu chứng','điều trị','phẫu thuật','hội chứng','ngộ độc','xương','viêm','phổi'],weak:['bệnh','thuốc','đau','máu','tim','gan','thận','nhiễm','bác sĩ','bệnh nhân','cơ bắp','đầu gối']},
 HOME:{strong:['đồ gia dụng','nội thất','nhà ở','phòng ngủ','phòng khách','nhà bếp','tủ lạnh'],weak:['bàn','ghế','giường','chăn','tủ','bếp','cửa','mái nhà'],exclude:['nhà văn','nhà thơ','nhà khoa học','nhà nước','nhà máy','nhà báo']},
 LANGUAGE:{strong:['ngôn ngữ','ngữ pháp','từ vựng','câu văn','dịch thuật'],weak:['danh từ','động từ','tính từ','trạng từ','đại từ','âm tiết','chính tả']},
 LAW_GOVERNMENT:{strong:['luật pháp','chính phủ','chính trị','tòa án','cảnh sát','tội phạm','bầu cử','hiến pháp','tội','bỏ tù','người tù'],weak:['luật','tù','bắt giữ','thẩm phán','quốc hội','bỏ phiếu'],exclude:['tội nghiệp','tù trưởng','tù túng','ao tù','rửa tội','tội lỗi']},
 LEISURE:{strong:['giải trí','sở thích','trò chơi','thời gian rảnh'],weak:['chơi','đồ chơi','câu đố']},
 NATURE:{strong:['thiên nhiên','môi trường','địa lý','động đất','núi lửa','sông ngòi','rừng cây','đại dương'],weak:['cây','hoa','rừng','núi','sông','biển','đất','đá','thung lũng','thác nước'],exclude:['đá bóng','nước đá','đá quý']},
 PERSONAL:{strong:['thông tin cá nhân','ngoại hình','tính cách','họ tên'],weak:['tên','tuổi','địa chỉ','danh tính']},
 RELATIONSHIPS:{strong:['mối quan hệ','tình bạn','quan hệ xã hội','đồng nghiệp'],weak:['bạn bè','hàng xóm','đối tác']},
 SCHOOL:{strong:['giáo dục','trường học','học sinh','giáo viên','kỳ thi'],weak:['học','trường','thi','lớp học','bài giảng'],exclude:['vệ sinh viên']},
 SCIENCE:{strong:['khoa học','toán học','vật lý','hóa học','sinh học','thí nghiệm'],weak:['nghiên cứu','phòng thí nghiệm','nhà khoa học']},
 SHOPPING:{strong:['mua sắm','cửa hàng bán','khách hàng','giá bán','hóa đơn mua hàng'],weak:['mua','bán','hàng hóa','siêu thị']},
 SPORTS:{strong:['thể thao','vận động viên','thi đấu','bóng đá','quần vợt','tập thể dục'],weak:['trận đấu','chạy đua','bơi','đội bóng','sân vận động']},
 TECHNOLOGY:{strong:['công nghệ','máy tính','phần mềm','phần cứng','mạng internet','thiết bị điện tử'],weak:['dữ liệu','kỹ thuật số','lập trình','trang web','bàn phím']},
 TIME_CALENDAR:{strong:['thời gian','lịch','giờ phút','ngày tháng','tuần lễ'],weak:['giờ','phút','ngày','tháng','năm','tuần','buổi sáng','buổi chiều','hôm qua','hôm nay']},
 TRANSPORTATION:{strong:['giao thông','phương tiện giao thông','xe cộ','tàu hỏa','đường sắt','máy bay'],weak:['xe','tàu','lái xe','đường bộ']},
 TRAVEL:{strong:['du lịch','hành trình','khách sạn','sân bay','hộ chiếu','hành lý'],weak:['chuyến đi','đặt phòng','vé máy bay','khách du lịch']},
 WEATHER:{strong:['thời tiết','khí hậu','mưa bão','dự báo thời tiết','sương'],weak:['mưa','gió','bão','tuyết','nhiệt độ','sấm','chớp']},
 WORK:{strong:['công việc','nghề nghiệp','nhân viên','văn phòng','nơi làm việc','nghề'],weak:['công ty','lao động','tuyển dụng','thất nghiệp','người làm']},
};

const MORPHOLOGY:Array<{code:string;patterns:RegExp[]}>= [
 {code:'ARTS',patterns:[/^(?:music|musical|paint|sculpt|literat|poet|theatr|cinema|ballet|orchestr)/,/^opera(?:$|tic)/]},
 {code:'CLOTHING',patterns:[/^(?:cloth|garment|textile|trouser|jacket|shirt|skirt|sweater|footwear|headwear)/]},
 {code:'COMMUNICATION',patterns:[/^(?:journalis|broadcast|telephon|telegraph|newspaper|postal)/]},
 {code:'FAMILY',patterns:[/^(?:maternal|paternal|fraternal|marital|kinship)/]},
 {code:'FINANCE',patterns:[/^(?:financ|econom|fiscal|monetar|mortgag|salar|taxat|wage)/,/^invest(?:$|ment|or|ing|ed|s)/,/^insur(?:e|ance|ed|ing)$/,/^bank(?:ing|note|rupt)/]},
 {code:'FOOD',patterns:[/^(?:culinar|edible|beverage|vegetable|confection|bakery|gastronom)/]},
 {code:'HEALTH',patterns:[/(?:itis|ectomy|algia|emia)$/,/^(?:cardio|nephro|hepato|dermato|osteo|medical|surgical|therapeut)/]},
 {code:'LANGUAGE',patterns:[/^(?:linguist|grammat|lexic|phonetic|phonolog|syllab|etymolog|vocabular)/]},
 {code:'LAW_GOVERNMENT',patterns:[/^(?:criminal|legislat|judici|juris|constitutional|electoral|parliament|governmental|politic)/]},
 {code:'SCHOOL',patterns:[/^(?:educat|pedagog|academ|scholastic|classroom)/]},
 {code:'SCIENCE',patterns:[/^(?:biolog|chemist|chemistry|astronom|mathemat|scientif|laborator)/,/^physic(?:s|al|ist)$/]},
 {code:'SPORTS',patterns:[/^(?:athlet|sport|football|basketball|volleyball|tennis|boxing|cricket|rugby|soccer|hockey)/]},
 {code:'TECHNOLOGY',patterns:[/^(?:algorithm|computer|cyber|digital|electronic|hardware|software|database|robotic)/]},
 {code:'TIME_CALENDAR',patterns:[/^(?:temporal|hourly|daily|weekly|monthly|yearly|annual|calendar)/]},
 {code:'TRANSPORTATION',patterns:[/^(?:railroad|railway|automobile|vehicular|aviation|motorcycle)/]},
 {code:'WEATHER',patterns:[/^(?:meteorolog|climatic|rainfall|snowfall|thunderstorm)/]},
];

// Corrections found during the deterministic per-topic sample review. Null means the sense is
// genuinely ambiguous for the available 29 topics and must remain in its POS fallback.
const AUDIT_OVERRIDES=new Map<number,string|null>([
 [7902,'SPORTS'],[9826,null],
 [6566,'FAMILY'],[10221,'CULTURE'],[10418,'CULTURE'],[14166,'CULTURE'],[15459,'CULTURE'],[15639,'CULTURE'],[17815,'CULTURE'],
 [16823,null],
 [7433,null],[10478,null],[11075,null],[15821,null],[16751,null],[17751,null],
 [13461,'COMMUNICATION'],
 [11383,null],[11827,null],[14245,null],[15338,'SCIENCE'],
 [18049,'ANIMALS'],[18439,'SCHOOL'],
 [14350,null],[15755,null],[19690,null],[11858,null],[18493,null],[12624,null],[19110,'SCIENCE'],
 [6408,'CULTURE'],[12258,'CULTURE'],[13488,'CULTURE'],[15668,'CULTURE'],[19508,'CULTURE'],
 [6492,null],[9407,null],[10269,null],[13048,null],[17843,null],[18059,null],
 [9274,'SPORTS'],[11654,'ANIMALS'],[14972,'FINANCE'],[15358,'HEALTH'],
]);

const STOP=new Set('a an the and or but if then than of to in on at by for from with without into onto over under is are was were be been being this that these those it its as about not no do does did have has had can could may might must shall should will would very more most less least much many some any all each every other another one two first last new old good bad big small long short high low same different such own also just only even still already again ever never always often usually sometimes here there where when how why what which who whom whose i you he she we they me him her us them my your his our their một những các là và hoặc của cho trong trên dưới với từ về được bị có không rất nhiều ít này đó đây kia như khi nơi cách vì gì ai nào từng mỗi khác cũng chỉ đã sẽ đang lại còn hãy'.split(/\s+/));
const normalize=(v:string)=>v.normalize('NFC').toLocaleLowerCase('vi-VN').replace(/[’']/g,"'");
const words=(v:string)=>normalize(v).match(/[\p{L}\p{N}]+/gu)??[];
const phraseContains=(text:string,term:string)=>` ${words(text).join(' ')} `.includes(` ${words(term).join(' ')} `);
const termCache=new Map<string,string>();
const normalizedTerm=(term:string)=>{let value=termCache.get(term);if(!value){value=` ${words(term).join(' ')} `;termCache.set(term,value);}return value;};
const features=(row:Pick<Row,'english'|'vietnamese'>):string[]=>{
 const en=words(row.english).filter(t=>t.length>1&&!STOP.has(t)),vi=words(row.vietnamese).filter(t=>t.length>1&&!STOP.has(t));
 const out=[...en.map(t=>`e:${t}`),...vi.map(t=>`v:${t}`)];
 for(let i=0;i+1<vi.length;i++)out.push(`v2:${vi[i]} ${vi[i+1]}`);
 return [...new Set(out)];
};
function manual(row:Row):Mapping|undefined{
 const head=normalize(row.english).trim(),sense=` ${words(`${row.english} ${row.vietnamese} ${row.source_topics.join(' ')}`).join(' ')} `,has=(term:string)=>sense.includes(normalizedTerm(term));
 const ranked:Array<{code:string;score:number;evidence:string[];strong:boolean}>=[];
 for(const rule of RULES){const signal=SIGNALS[rule.code]!;if(signal.exclude?.some(has)||rule.exclude?.some(has))continue;
  const evidence:string[]=[];let score=0,strong=false;
  if(SAFE_HEADS[rule.code]?.has(head)){score+=6;strong=true;evidence.push(`head:${head}`);}
  for(const hit of [...new Set([...rule.sense,...signal.strong])].filter(has)){score+=4;strong=true;evidence.push(`strong:${hit}`);}
  const weak=signal.weak.filter(has);for(const hit of weak){score+=1.5;evidence.push(`weak:${hit}`);}
  if(rule.heads.includes(head)&&!SAFE_HEADS[rule.code]?.has(head)){score+=1.5;evidence.push(`support-head:${head}`);}
  const morphology=MORPHOLOGY.find(item=>item.code===rule.code)?.patterns.find(pattern=>pattern.test(head));if(morphology){score+=5;strong=true;evidence.push(`morph:${morphology.source}`);}
  if(score>=4&&(strong||weak.length>=2))ranked.push({code:rule.code,score,evidence,strong});
 }
 ranked.sort((a,b)=>b.score-a.score||a.code.localeCompare(b.code));const best=ranked[0],second=ranked[1];if(!best||second&&best.score-second.score<1.5)return undefined;
 return {flashcardId:row.id,english:row.english,vietnamese:row.vietnamese,partOfSpeech:row.part_of_speech,oldTopic:row.old_topic,newTopic:best.code,reason:`SEMANTIC_RULE:${best.score.toFixed(1)}`,confidence:best.score>=6?'HIGH':'MEDIUM',evidence:best.evidence};
}
function learn(rows:Array<Row&{topic:string}>):Map<string,Learned>{
 const counts=new Map<string,Map<string,number>>(),docs=new Map<string,number>();
 for(const row of rows){for(const f of features(row)){let by=counts.get(f);if(!by){by=new Map();counts.set(f,by);}by.set(row.topic,(by.get(row.topic)??0)+1);docs.set(f,(docs.get(f)??0)+1);}}
 const learned=new Map<string,Learned>();
 for(const [f,by] of counts){const total=docs.get(f)??0,sorted=[...by].sort((a,b)=>b[1]-a[1]);const [topic,count]=sorted[0]!,second=sorted[1]?.[1]??0,purity=count/total;
  if(count>=3&&purity>=0.82&&count>=second*3&&total<=Math.max(80,rows.length*0.05))learned.set(f,{topic,purity,count,weight:purity*Math.log2(1+count)});
 }
 return learned;
}
function learnedMapping(row:Row,model:Map<string,Learned>):Mapping|undefined{
 const scores=new Map<string,number>(),evidence=new Map<string,string[]>();
 for(const f of features(row)){const hit=model.get(f);if(!hit)continue;scores.set(hit.topic,(scores.get(hit.topic)??0)+hit.weight);const list=evidence.get(hit.topic)??[];list.push(`${f}:${hit.count}/${hit.purity.toFixed(2)}`);evidence.set(hit.topic,list);}
 const ranked=[...scores].sort((a,b)=>b[1]-a[1]);if(!ranked.length)return undefined;
 const [topic,score]=ranked[0]!,second=ranked[1]?.[1]??0,proof=evidence.get(topic)??[];
 const independent=new Set(proof.map(p=>p.replace(/^[ev]2?:/,'').split(':')[0]??'').flatMap(p=>p.split(' ')).filter(Boolean));
 const hasEnglish=proof.some(p=>p.startsWith('e:')),hasVietnamesePhrase=proof.some(p=>p.startsWith('v2:'));
 if(score<9||proof.length<3||independent.size<3||score<second*2.5||!hasEnglish||!hasVietnamesePhrase||topic==='FOOD')return undefined;
 return {flashcardId:row.id,english:row.english,vietnamese:row.vietnamese,partOfSpeech:row.part_of_speech,oldTopic:row.old_topic,newTopic:topic,reason:`VERIFIED_SEED_MODEL:${score.toFixed(2)}:${second.toFixed(2)}`,confidence:score>=8&&score>=second*2.5?'HIGH':'MEDIUM',evidence:proof.slice(0,8)};
}

const args=process.argv.slice(2),apply=args.includes('--apply'),confirm=args[args.indexOf('--database-confirm')+1];
const env=readDatabaseEnv(process.env);if(apply&&(!['127.0.0.1','localhost','::1'].includes(env.DB_HOST)||confirm!==env.DB_NAME))throw new Error('Apply requires local DB and exact confirmation');
const outDir=new URL('../.m15-7-verification/',import.meta.url),planUrl=new URL('../.m15-7-verification/semantic-mapping.jsonl',import.meta.url),csvUrl=new URL('../.m15-7-verification/semantic-mapping.csv',import.meta.url),summaryUrl=new URL('../.m15-7-verification/semantic-summary.json',import.meta.url),sampleUrl=new URL('../.m15-7-verification/semantic-sample.csv',import.meta.url);
const pool=new pg.Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,password:env.DB_PASSWORD,max:2});
try{
 if(!apply){
  const targets=(await pool.query<Row>(`SELECT f.id,f.english,f.vietnamese,f.part_of_speech,t.code old_topic,'{}'::text[] source_topics
   FROM flashcards f JOIN topics t ON t.id=f.topic_id
   WHERE t.code=ANY($1::text[]) ORDER BY f.id`,[POS_TOPICS])).rows;
  if(targets.length!==12858)throw new Error(`Expected 12858 POS cards, got ${targets.length}`);
  const seeds=(await pool.query<Row&{topic:string}>(`SELECT f.id,f.english,f.vietnamese,f.part_of_speech,t.code old_topic,t.code topic,'{}'::text[] source_topics
   FROM flashcards f JOIN topics t ON t.id=f.topic_id
   WHERE t.code<>'GENERAL_VOCABULARY' AND t.code<>ALL($1::text[])
   AND NOT EXISTS(SELECT 1 FROM flashcard_provenance fp WHERE fp.flashcard_id=f.id AND fp.validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb)
   ORDER BY f.id`,[POS_TOPICS])).rows;
  const model=learn(seeds),mappings:Mapping[]=[];
  for(const row of targets){const override=AUDIT_OVERRIDES.get(row.id);if(AUDIT_OVERRIDES.has(row.id)){if(override)mappings.push({flashcardId:row.id,english:row.english,vietnamese:row.vietnamese,partOfSpeech:row.part_of_speech,oldTopic:row.old_topic,newTopic:override,reason:'AUDIT_OVERRIDE',confidence:'HIGH',evidence:['reviewed English + Vietnamese sense']});continue;}const mapped=manual(row)??learnedMapping(row,model);if(mapped)mappings.push(mapped);}
  const text=mappings.map(v=>JSON.stringify(v)).join('\n')+'\n';await mkdir(fileURLToPath(outDir),{recursive:true});await writeFile(planUrl,text);
  const quote=(v:unknown)=>`"${String(v??'').replaceAll('"','""')}"`;await writeFile(csvUrl,['flashcard_id,english,vietnamese,pos,old_topic,new_topic,reason,confidence,evidence',...mappings.map(m=>[m.flashcardId,m.english,m.vietnamese,m.partOfSpeech,m.oldTopic,m.newTopic,m.reason,m.confidence,m.evidence.join('|')].map(quote).join(','))].join('\n')+'\n');
  const byTopic=Object.fromEntries(RULES.map(r=>r.code).sort().map(code=>[code,mappings.filter(m=>m.newTopic===code).length]));
  const before=Object.fromEntries(POS_TOPICS.map(code=>[code,targets.filter(r=>r.old_topic===code).length]));
  const after=Object.fromEntries(POS_TOPICS.map(code=>[code,targets.filter(r=>r.old_topic===code).length-mappings.filter(m=>m.oldTopic===code).length]));
  const samples:string[]=['flashcard_id,english,vietnamese,pos,old_topic,new_topic,reason,confidence,evidence'];
  for(const code of Object.keys(byTopic))for(const m of mappings.filter(x=>x.newTopic===code).sort((a,b)=>a.flashcardId-b.flashcardId).slice(0,20))samples.push([m.flashcardId,m.english,m.vietnamese,m.partOfSpeech,m.oldTopic,m.newTopic,m.reason,m.confidence,m.evidence.join('|')].map(quote).join(','));
  await writeFile(sampleUrl,samples.join('\n')+'\n');
  const summary={database:env.DB_NAME,sourcePosCards:targets.length,verifiedSeeds:seeds.length,learnedFeatures:model.size,moved:mappings.length,remaining:targets.length-mappings.length,coverage:Number((mappings.length/targets.length*100).toFixed(2)),byTopic,beforePos:before,afterPos:after,confidence:{high:mappings.filter(m=>m.confidence==='HIGH').length,medium:mappings.filter(m=>m.confidence==='MEDIUM').length},method:{rules:mappings.filter(m=>m.reason.startsWith('SEMANTIC_')).length,learned:mappings.filter(m=>m.reason.startsWith('VERIFIED_')).length,auditOverrides:mappings.filter(m=>m.reason==='AUDIT_OVERRIDE').length},sha256:createHash('sha256').update(text).digest('hex')};
  await writeFile(summaryUrl,JSON.stringify(summary,null,2)+'\n');console.log(JSON.stringify(summary,null,2));process.exit(0);
 }
 const text=await readFile(planUrl,'utf8'),mappings=text.split(/\r?\n/).filter(Boolean).map(line=>JSON.parse(line) as Mapping);if(!mappings.length)throw new Error('Frozen mapping is empty');
 const current=(await pool.query<{pos:number;total:number}>(`SELECT (SELECT count(*)::int FROM flashcards) total,(SELECT count(*)::int FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.code=ANY($1::text[])) pos`,[POS_TOPICS])).rows[0]!;
 if(current.total!==20000||![12858,12858-mappings.length].includes(current.pos))throw new Error(`Unexpected database state ${JSON.stringify(current)}`);
 if(current.pos===12858-mappings.length){console.log(JSON.stringify({database:env.DB_NAME,moved:0,idempotent:true}));process.exit(0);}
 const client=await pool.connect();try{await client.query('BEGIN');
  for(let offset=0;offset<mappings.length;offset+=500){const batch=mappings.slice(offset,offset+500);const ids=batch.map(x=>x.flashcardId);
   const valid=(await client.query<{count:number}>(`SELECT count(*)::int count FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE f.id=ANY($1::int[]) AND t.code=ANY($2::text[])`,[ids,POS_TOPICS])).rows[0]!.count;if(valid!==batch.length)throw new Error(`Frozen plan/current topic mismatch at offset ${offset}`);
   await client.query(`WITH input AS (SELECT (v->>'flashcardId')::int id,v->>'newTopic' code FROM jsonb_array_elements($1::jsonb)v), targets AS (SELECT i.id,t.id topic_id FROM input i JOIN topics t ON t.code=i.code), cleared AS (UPDATE flashcard_topics ft SET is_primary=FALSE FROM targets x WHERE ft.flashcard_id=x.id RETURNING ft.flashcard_id), linked AS (INSERT INTO flashcard_topics(flashcard_id,topic_id,is_primary) SELECT x.id,x.topic_id,TRUE FROM targets x JOIN (SELECT DISTINCT flashcard_id FROM cleared)c ON c.flashcard_id=x.id ON CONFLICT(flashcard_id,topic_id) DO UPDATE SET is_primary=TRUE RETURNING flashcard_id) UPDATE flashcards f SET topic_id=x.topic_id,updated_at=CURRENT_TIMESTAMP FROM targets x JOIN linked l ON l.flashcard_id=x.id WHERE f.id=x.id`,[JSON.stringify(batch)]);
   await client.query(`DELETE FROM flashcard_topics ft USING topics t WHERE ft.topic_id=t.id AND t.code=ANY($2::text[]) AND ft.flashcard_id=ANY($1::int[])`,[ids,POS_TOPICS]);
  }
  await client.query('COMMIT');
 }catch(error){await client.query('ROLLBACK');throw error;}finally{client.release();}
 console.log(JSON.stringify({database:env.DB_NAME,moved:mappings.length,remainingPos:12858-mappings.length,planSha256:createHash('sha256').update(text).digest('hex'),status:'APPLIED'},null,2));
}finally{await pool.end();}
