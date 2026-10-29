"""Persist the AI-assisted semantic review of the independent final 500-card sample."""
from __future__ import annotations
import csv, json
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'.m15-5-verification'/'quality-sample-500-final.json'
JSON_OUT=ROOT/'.m15-5-verification'/'quality-review-500-final.json'
CSV_OUT=ROOT/'.m15-5-verification'/'quality-review-500-final.csv'

# These are content findings from reading all 500 rows. This is explicitly
# AI-assisted review, not human verification and not merely a schema check.
REVIEW={
 'fluid':('VIETNAMESE_MEANING_PARTLY_WRONG',),
 'you':('VIETNAMESE_MEANING_MISMATCH',),
 'differently':('MEANING_NOT_VIETNAMESE',),
 'objective':('VIETNAMESE_TYPO',),
 'boiler':('MALFORMED_VIETNAMESE_MEANING',),
 'secretly':('POS_MEANING_MISMATCH',),
 'ecocide':('VIETNAMESE_MEANING_MISMATCH',),
 'sewing':('VIETNAMESE_TYPO',),
 'scour':('VIETNAMESE_TYPO',),
 'body':('MALFORMED_DICTIONARY_DEFINITION',),
 'hyphen':('POS_MEANING_MISMATCH',),
 'ream':('MALFORMED_DICTIONARY_DEFINITION',),
 'moral':('VIETNAMESE_MEANING_MISMATCH',),
 'isolation':('INCOMPLETE_VIETNAMESE_MEANING',),
 'wrist':('VIETNAMESE_MEANING_MISMATCH',),
 'altruistic':('POS_MEANING_MISMATCH',),
 'amber':('POS_MEANING_MISMATCH',),
 'alum':('POS_MEANING_MISMATCH',),
 'medical':('POS_MEANING_MISMATCH',),
 'label':('DICTIONARY_METADATA_IN_MEANING',),
 'monarchy':('DICTIONARY_METADATA_IN_MEANING',),
}
payload=json.loads(SOURCE.read_text(encoding='utf-8'))
rows=[]
for row in payload['rows']:
    reasons=list(REVIEW.get(str(row['english']).casefold(),()))
    rows.append({**row,'reviewVerdict':'NEEDS_REVIEW' if reasons else 'PASS_IN_SAMPLE',
                 'reviewReasons':reasons})
verdicts=Counter(row['reviewVerdict'] for row in rows)
reasons=Counter(reason for row in rows for reason in row['reviewReasons'])
summary={**payload['summary'],'reviewMethod':'AI-assisted field-by-field semantic review; human-reviewed count: 0',
         'verdicts':dict(sorted(verdicts.items())),'reviewReasons':dict(sorted(reasons.items())),
         'qualityGate':'FAIL'}
JSON_OUT.write_text(json.dumps({'summary':summary,'rows':rows},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
with CSV_OUT.open('w',encoding='utf-8-sig',newline='') as stream:
    fields=['id','topic','part_of_speech','level','english','vietnamese','independent_meaning_confirmed','reviewVerdict','reviewReasons']
    writer=csv.DictWriter(stream,fieldnames=fields);writer.writeheader()
    for row in rows: writer.writerow({**{key:row.get(key) for key in fields},'reviewReasons':';'.join(row['reviewReasons'])})
print(json.dumps(summary,ensure_ascii=False,indent=2))
