import { z } from 'zod';

/** Unmounted v2 contracts. Legacy Quiz schemas/routes remain unchanged. */
export const quizMethodSchema = z.enum(['MIXED','CHALLENGE','CONTEXT','EN_TO_VI','VI_TO_EN','MATCHING']);
const code = z.string().regex(/^[A-Z][A-Z0-9_]{0,49}$/);
const filter = z.object({topicCode:code.optional(),posCode:code.regex(/^POS_/).optional()}).strict()
  .refine(value => !(value.topicCode && value.posCode), 'Choose topic OR POS');
export const sessionCreateSchema = z.object({
  method:quizMethodSchema, filter,
  count:z.number().int().min(1).max(100),
  useAllAvailable:z.boolean().optional(),
}).strict().refine(value => value.useAllAvailable === true
  ? value.count < 20 : [20,40,60,80,100].includes(value.count), 'Invalid count preset/fallback');
export const sessionAnswerSchema = z.object({
  actionId:z.uuid(),questionId:z.uuid(),selectedOptionId:z.uuid(),
}).strict();

export interface IsolatedQuizTarget {
  host:string;port:number;database:string;backendPort:number;
  systemIdentifier:string;expectedSystemIdentifier:string;connectionUrl:string;
}
/** Must run before any writes, after inspecting the connected instance identity. */
export function assertIsolatedQuizTarget(target:IsolatedQuizTarget):void {
  let url:URL;
  try { url=new URL(target.connectionUrl); } catch { throw new Error('Unsafe quiz test target'); }
  if (target.host!=='127.0.0.1' || target.port!==55433 || target.database!=='bivoca_m16_7_quiz_test'
      || target.backendPort!==3001 || !/^\d{10,}$/.test(target.expectedSystemIdentifier)
      || target.systemIdentifier!==target.expectedSystemIdentifier
      || !['postgresql:','postgres:'].includes(url.protocol) || url.hostname!==target.host
      || url.port!=='55433' || url.pathname!=='/bivoca_m16_7_quiz_test' || url.search || url.hash) {
    throw new Error('Unsafe quiz test target');
  }
}
