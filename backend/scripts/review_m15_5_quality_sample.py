"""Persist the human review verdicts for the deterministic M15.5 quality sample."""
from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / ".m15-5-verification" / "quality-sample-240.json"
JSON_OUT = ROOT / ".m15-5-verification" / "quality-review-240.json"
CSV_OUT = ROOT / ".m15-5-verification" / "quality-review-240.csv"

# These verdicts come from reading the English, Vietnamese, POS, IPA and example
# together. They intentionally do not treat structural validation as human review.
REVIEW: dict[str, tuple[str, ...]] = {
    "centennial": ("POS_MEANING_MISMATCH",),
    "mulmull": ("VIETNAMESE_MEANING_SUSPECT",),
    "disappropriate": ("EXAMPLE_UNNATURAL",),
    "lived": ("INFLECTION_ONLY_MEANING",),
    "life-office": ("VIETNAMESE_TRUNCATED",),
    "archdeaconry": ("IPA_SUSPECT",),
    "heart-whole": ("MEANING_NOT_VIETNAMESE", "IPA_SUSPECT"),
    "eighteenth": ("EXAMPLE_UNNATURAL",),
    "symbol": ("POS_OR_HEADWORD_FORM_SUSPECT", "EXAMPLE_UNGRAMMATICAL"),
    "floriation": ("POS_MEANING_MISMATCH", "EXAMPLE_UNGRAMMATICAL"),
    "middlings": ("MEANING_EXAMPLE_MISMATCH",),
    "diastase": ("VIETNAMESE_MEANING_NOT_EXPLANATORY",),
    "ligroin": ("VIETNAMESE_MEANING_NOT_EXPLANATORY",),
    "ephemeron": ("VIETNAMESE_MEANING_NOT_EXPLANATORY",),
    "fair-maid": ("EXAMPLE_MEANINGLESS",),
    "nonius": ("VIETNAMESE_MEANING_SUSPECT",),
    "stomach": ("ARCHAIC_SENSE_AND_EXAMPLE",),
    "trod": ("POS_OR_MEANING_SUSPECT",),
    "immaterialist": ("IPA_SUSPECT",),
    "sprang": ("POS_OR_MEANING_SUSPECT",),
    "quadratus": ("VIETNAMESE_MEANING_SUSPECT",),
    "phlegmatic": ("MEANING_EXAMPLE_MISMATCH",),
    "grub": ("VIETNAMESE_TYPO_OR_MISMATCH",),
    "an": ("EXAMPLE_UNGRAMMATICAL",),
    "instant": ("POS_EXAMPLE_MISMATCH",),
    "electroretinogram": ("IPA_SUSPECT",),
    "quiz": ("MEANING_EXAMPLE_MISMATCH",),
    "computer theory": ("EXAMPLE_MEANINGLESS",),
    "sponsor": ("MEANING_EXAMPLE_MISMATCH",),
    "neurotic": ("STIGMATIZING_LEARNER_SENSE",),
    "photophobic": ("IPA_SUSPECT",),
    "histrionically": ("EXAMPLE_UNNATURAL",),
    "hexametrical": ("VIETNAMESE_MEANING_SUSPECT", "IPA_SUSPECT"),
    "urachus": ("VIETNAMESE_TYPO",),
    "sport": ("EXAMPLE_MEANINGLESS",),
    "hindoo": ("OUTDATED_OR_SENSITIVE_TERM",),
    "uncleansed": ("EXAMPLE_UNNATURAL",),
    "smoke": ("MEANING_EXAMPLE_MISMATCH",),
    "portion": ("MEANING_EXAMPLE_MISMATCH",),
    "traveling": ("EXAMPLE_UNNATURAL",),
    "disembarkment": ("VIETNAMESE_MEANING_SUSPECT", "IPA_SUSPECT"),
    "cats": ("UNEXPANDED_ACRONYM",),
    "revenant": ("EXAMPLE_MEANINGLESS",),
    "extravagantly": ("EXAMPLE_UNNATURAL",),
    "meed": ("EXAMPLE_MEANINGLESS",),
    "ulsterman": ("VIETNAMESE_TYPO",),
    "secondary": ("MEANING_EXAMPLE_MISMATCH",),
    "cohomology": ("VIETNAMESE_MEANING_MALFORMED",),
    "zincograph": ("VIETNAMESE_MEANING_NOT_EXPLANATORY", "IPA_SUSPECT"),
    "outpass": ("POS_EXAMPLE_MISMATCH",),
    "oecumenical": ("IPA_SUSPECT",),
    "litigious": ("EXAMPLE_UNNATURAL",),
    "reporting": ("INFLECTION_ONLY_MEANING",),
}

payload = json.loads(SOURCE.read_text(encoding="utf-8"))
rows = payload["rows"]
present = {str(row["english"]).casefold() for row in rows}
missing = sorted(set(REVIEW) - present)
if missing:
    raise RuntimeError(f"Review keys are absent from deterministic sample: {missing}")

reviewed = []
for row in rows:
    key = str(row["english"]).casefold()
    reasons = list(REVIEW.get(key, ()))
    reviewed.append({**row, "humanVerdict": "NEEDS_REVIEW" if reasons else "PASS",
                     "humanReasons": reasons})

verdicts = Counter(row["humanVerdict"] for row in reviewed)
reasons = Counter(reason for row in reviewed for reason in row["humanReasons"])
summary = {**payload["summary"], "reviewMethod": "manual field-by-field review",
           "verdicts": dict(sorted(verdicts.items())), "reviewReasons": dict(sorted(reasons.items()))}
JSON_OUT.write_text(json.dumps({"summary": summary, "rows": reviewed}, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8")
with CSV_OUT.open("w", encoding="utf-8-sig", newline="") as stream:
    writer = csv.DictWriter(stream, fieldnames=["id", "topic", "part_of_speech", "level", "english",
        "vietnamese", "pronunciation", "example", "humanVerdict", "humanReasons"])
    writer.writeheader()
    for row in reviewed:
        writer.writerow({**{key: row.get(key) for key in writer.fieldnames},
                         "humanReasons": ";".join(row["humanReasons"])})
print(json.dumps(summary, ensure_ascii=False, indent=2))
