export interface ReviewCandidateRow {
  candidate_id:string; english:string; vietnamese:string; pronunciation:string|null; part_of_speech:string|null;
  cefr:'A1'|'A2'|'B1'|'B2'|null; primary_topic_code:string|null; topic_codes:string[]; example:string|null;
  example_vi:string|null; source_code:string; source_name:string; source_url:string; source_version:string;
  source_license:string; source_record_id:string; validation_reasons:string[]; quality_score:number;
  quality_verified:boolean; license_status:'ATTRIBUTION_REQUIRED'|'VERIFIED'|'BLOCKED';
  status:'PENDING'|'APPROVED'|'REJECTED'|'PUBLISHED'; reviewed_by:number|null; reviewer_username:string|null;
  reviewed_at:Date|null; published_flashcard_id:number|null; row_version:number; updated_at:Date;
  exact_duplicate_id:number|null; same_word_count:number;
}
export interface ReviewPage { rows:ReviewCandidateRow[]; total:string }

