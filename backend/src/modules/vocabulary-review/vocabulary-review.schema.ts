import { z } from 'zod';

const canonical = z.string().regex(/^[1-9]\d*$/).transform(Number).pipe(z.number().int().min(1).max(2147483647));
const nullableText = (max: number) => z.string().trim().max(max).nullable();
export const candidateId = z.string().trim().regex(/^bivoca-[a-z0-9-]{8,79}$/);
export const batchId = z.uuid();
export const level = z.enum(['A1','A2','B1','B2']);
export const reviewStatus = z.enum(['PENDING','APPROVED','REJECTED','PUBLISHED']);
export const topicCode = z.string().trim().max(50).regex(/^[A-Z][A-Z0-9_]*$/);
export const reviewQuery = z.object({
  page: canonical.optional().default(1), limit: canonical.pipe(z.number().max(100)).optional().default(20),
  search: z.string().trim().max(200).optional().default(''), level: level.optional(),
  topicCode: topicCode.optional(), partOfSpeech: z.string().trim().max(30).optional(),
  status: reviewStatus.optional(), reason: z.string().trim().max(100).optional(),
  source: z.string().trim().max(80).optional(),
});
export const editCandidateBody = z.object({
  english: z.string().trim().min(1).max(150), vietnamese: z.string().trim().min(1).max(255),
  pronunciation: nullableText(150), partOfSpeech: nullableText(30), cefr: level,
  primaryTopicCode: topicCode, topicCodes: z.array(topicCode).min(1).max(8),
  example: nullableText(5000), exampleVi: nullableText(5000),
  rowVersion: z.number().int().min(1),
}).strict().superRefine((value,ctx)=>{
  if (!value.topicCodes.includes(value.primaryTopicCode)) ctx.addIssue({code:'custom',message:'Primary topic must be selected'});
});
export const statusBody = z.object({ status: z.enum(['PENDING','APPROVED','REJECTED']), rowVersion:z.number().int().min(1) }).strict();
export const bulkBody = z.object({ candidateIds:z.array(candidateId).min(1).max(100), status:z.enum(['PENDING','APPROVED','REJECTED']) }).strict();
export const previewBody = z.object({ candidateIds:z.array(candidateId).min(1).max(100) }).strict();
export const confirmBody = z.object({ confirmation:z.literal('PUBLISH_APPROVED_BATCH') }).strict();
export type ReviewQuery = z.infer<typeof reviewQuery>;
export type EditCandidate = z.infer<typeof editCandidateBody>;
export type ReviewStatusInput = z.infer<typeof statusBody>;
export type BulkInput = z.infer<typeof bulkBody>;

