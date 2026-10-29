import { createHash, randomUUID } from 'node:crypto';
import { AppError } from '../../shared/errors/app-error.js';
import type { BulkInput, EditCandidate, ReviewQuery, ReviewStatusInput } from './vocabulary-review.schema.js';
import type { VocabularyReviewRepository } from './vocabulary-review.repository.js';
import type { ReviewCandidateRow } from './vocabulary-review.types.js';

const count=(v:string|number)=>{const n=Number(v);if(!Number.isSafeInteger(n)||n<0)throw new Error('Invalid review aggregate');return n;};
const iso=(v:Date|null)=>v?.toISOString()??null;
function dto(c:ReviewCandidateRow) {
  const duplicateType=c.exact_duplicate_id?'EXACT':c.same_word_count>0?'SAME_WORD_DIFFERENT_SENSE':'NONE';
  return {candidateId:c.candidate_id,english:c.english,vietnamese:c.vietnamese,pronunciation:c.pronunciation,
    partOfSpeech:c.part_of_speech,cefr:c.cefr,primaryTopicCode:c.primary_topic_code,topicCodes:c.topic_codes,
    example:c.example,exampleVi:c.example_vi,source:{code:c.source_code,name:c.source_name,url:c.source_url,
      version:c.source_version,license:c.source_license,recordId:c.source_record_id},validationReasons:c.validation_reasons,
    qualityScore:c.quality_score,qualityVerified:c.quality_verified,licenseStatus:c.license_status,status:c.status,
    reviewer:c.reviewer_username,reviewedAt:iso(c.reviewed_at),publishedFlashcardId:c.published_flashcard_id,
    rowVersion:c.row_version,updatedAt:c.updated_at.toISOString(),duplicate:{type:duplicateType,flashcardId:c.exact_duplicate_id,
      sameWordCount:c.same_word_count},multiSense:c.validation_reasons.includes('MULTI_SENSE_REQUIRES_HUMAN_REVIEW')};
}
function ensureApprovable(c:ReviewCandidateRow) {
  if(!c.english.trim()||!c.vietnamese.trim()||!c.part_of_speech?.trim()||!c.primary_topic_code||!c.topic_codes.includes(c.primary_topic_code))
    throw new AppError(409,'REVIEW_QUALITY_INCOMPLETE','English, meaning, part of speech and topic are required before approval');
  if(c.english.length>150||c.vietnamese.length>255||(c.pronunciation?.length??0)>150)
    throw new AppError(409,'REVIEW_FIELD_TOO_LONG','Reviewed content exceeds the Flashcard schema limits');
  if(c.example?.trim()&&!c.example.toLocaleLowerCase('en').includes(c.english.toLocaleLowerCase('en')))
    throw new AppError(409,'REVIEW_EXAMPLE_MISMATCH','Example must contain the reviewed English headword');
  if(c.license_status==='BLOCKED') throw new AppError(409,'REVIEW_LICENSE_BLOCKED','Source license blocks publishing');
}
export function createVocabularyReviewService(repository:VocabularyReviewRepository) {
  return {
    async stats(){const s=await repository.stats();return {total:count(s.total),pending:count(s.pending),approved:count(s.approved),rejected:count(s.rejected),published:count(s.published)};},
    filters:()=>repository.filters(),
    async list(q:ReviewQuery){const r=await repository.list(q);const total=count(r.total);return{items:r.rows.map(dto),page:q.page,limit:q.limit,total,totalPages:Math.ceil(total/q.limit)};},
    async one(id:string){const row=await repository.one(id);if(!row)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','Vocabulary candidate not found');return dto(row);},
    async edit(actor:number,id:string,input:EditCandidate){const row=await repository.edit(actor,id,input);if(row===undefined)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','Vocabulary candidate not found');if(row===null)throw new AppError(409,'REVIEW_VERSION_CONFLICT','Candidate changed; refresh and retry');return dto(row);},
    async status(actor:number,id:string,input:ReviewStatusInput){const current=await repository.one(id);if(!current)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','Vocabulary candidate not found');if(input.status==='APPROVED')ensureApprovable(current);const row=await repository.changeStatus(actor,id,input.status,input.rowVersion);if(row===null)throw new AppError(409,'REVIEW_VERSION_CONFLICT','Candidate changed or was published');if(!row)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','Vocabulary candidate not found');return dto(row);},
    async bulk(actor:number,input:BulkInput){const ids=[...new Set(input.candidateIds)];if(ids.length!==input.candidateIds.length)throw new AppError(400,'REVIEW_DUPLICATE_SELECTION','Duplicate candidate IDs');const rows=await repository.batchRows(ids);if(rows.length!==ids.length)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','One or more candidates were not found');if(input.status==='APPROVED')rows.forEach(ensureApprovable);const changed=await repository.bulkChangeStatus(actor,ids,input.status);if(changed===null)throw new AppError(409,'REVIEW_ALREADY_PUBLISHED','Published candidates cannot change review state');if(changed===undefined)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','One or more candidates were not found');return{changed,status:input.status};},
    async preview(actor:number,idsInput:string[]){const ids=[...new Set(idsInput)].sort();if(ids.length!==idsInput.length)throw new AppError(400,'REVIEW_DUPLICATE_SELECTION','Duplicate candidate IDs');const rows=await repository.batchRows(ids);if(rows.length!==ids.length)throw new AppError(404,'REVIEW_CANDIDATE_NOT_FOUND','One or more candidates were not found');
      const items=rows.map(c=>{let disposition='INSERT',reason:string|null=null;if(c.status!=='APPROVED'){disposition='BLOCKED';reason='NOT_APPROVED';}else{try{ensureApprovable(c);}catch(e){disposition='BLOCKED';reason=e instanceof AppError?e.code:'QUALITY_BLOCKED';}}if(disposition==='INSERT'&&c.exact_duplicate_id){disposition='SKIP_DUPLICATE';reason='EXACT_EXISTING_FLASHCARD';}return{id:c.candidate_id,version:c.row_version,disposition,duplicateId:c.exact_duplicate_id,reason};});
      const fingerprint=createHash('sha256').update(items.map(i=>`${i.id}:${i.version}:${i.disposition}`).join('|')).digest('hex');
      const preview={total:items.length,toInsert:items.filter(i=>i.disposition==='INSERT').length,toSkip:items.filter(i=>i.disposition==='SKIP_DUPLICATE').length,blocked:items.filter(i=>i.disposition==='BLOCKED').length,items};
      const saved=await repository.savePreview(actor,{id:randomUUID(),fingerprint,preview,items});return{batchId:saved.batch_id,status:saved.status,...saved.preview};},
    async publish(actor:number,batchId:string){try{const row=await repository.publish(actor,batchId);if(row===undefined)throw new AppError(404,'PUBLISH_BATCH_NOT_FOUND','Publish batch not found');if(row===null)throw new AppError(409,'PUBLISH_BATCH_STATE','Publish batch is not confirmable');if(row.forbidden)throw new AppError(403,'PUBLISH_ACTOR_MISMATCH','The admin who created the preview must confirm it');if(row.blocked)throw new AppError(409,'PUBLISH_BATCH_BLOCKED','Preview contains blocked candidates');return{batchId:row.batch_id,status:row.status,inserted:row.inserted_count,skipped:row.skipped_count,blocked:row.blocked_count,completedAt:row.completed_at?.toISOString?.()??row.completed_at??null};}catch(e){if(typeof e==='object'&&e!==null&&'code'in e&&e.code==='M13_REVIEW_STALE')throw new AppError(409,'PUBLISH_PREVIEW_STALE','Candidate changed after preview; create a new preview');throw e;}},
    async audit(id:string,page:number,limit:number){await this.one(id);const r=await repository.auditPage(id,page,limit),total=count(r.total);return{items:r.rows,page,limit,total,totalPages:Math.ceil(total/limit)};},
  };
}
export type VocabularyReviewService=ReturnType<typeof createVocabularyReviewService>;

