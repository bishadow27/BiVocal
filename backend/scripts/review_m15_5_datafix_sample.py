"""Persist the independent AI-assisted review of the post-DataFix 500-card sample."""
from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / ".m15-5-verification" / "quality-sample-500-datafix.json"
JSON_OUT = ROOT / ".m15-5-verification" / "quality-review-500-datafix.json"
CSV_OUT = ROOT / ".m15-5-verification" / "quality-review-500-datafix.csv"

# Exact content keys avoid accidentally labelling another sense of the same headword.
# These findings came from reading the new 500-card sample, not from the ZIP annotations.
FINDINGS = {
    ("and", "conjunction", "Càng."): ("INCOMPLETE_VIETNAMESE_MEANING",),
    ("polish", "noun", "Tiếng Ba Lan."): ("HEADWORD_CASE_OR_SENSE_MISMATCH",),
    ("admiration", "noun", "Người được khâm phục, người được hâm mộ, người được ca tụng; vật được hâm mô, vật được ca tụng."): ("VIETNAMESE_TYPO",),
    ("subconscious", "adjective", "Tiềm thức."): ("POS_MEANING_MISMATCH",),
    ("deceive", "verb", "Làm thất vọng."): ("VIETNAMESE_MEANING_MISMATCH",),
    ("need", "verb", "Cần thiết."): ("POS_MEANING_MISMATCH",),
    ("australian", "adjective", "(thuộc) Úc."): ("HEADWORD_CASE_MISMATCH",),
    ("vietnam", "noun", "Việt Nam (quốc gia nằm phía đông của bán đảo Đông Dương, thuộc khu vực Đông Nam Á, giáp giới với nước Trung Quốc ở phía bắc, Biển Đông ở phía đông và nam, Vịnh Thái Lan ở phía tây nam, với lại Campuchia và Lào ở phía tây)"): ("HEADWORD_CASE_MISMATCH",),
    ("who", "noun", "Tổ chức Y tế Thế giới"): ("ACRONYM_CASE_MISMATCH",),
    ("track", "verb", "Đóng trăn, tra tấn (ai)."): ("VIETNAMESE_MEANING_MISMATCH", "VIETNAMESE_TYPO"),
    ("holy", "adjective", "Holy Writ kinh thánh."): ("DICTIONARY_METADATA_IN_MEANING",),
    ("gun", "noun", "Lóng súng lục."): ("VIETNAMESE_TYPO",),
    ("puritan", "noun", "(Purian) người theo Thanh giáo."): ("DICTIONARY_METADATA_IN_MEANING", "ENGLISH_TYPO"),
    ("pasteboard", "noun", "Bằng bìa cứng, bằng giấy bồi; (nghĩa bóng) không bền, không chắc chắn, mỏng mảnh."): ("POS_MEANING_MISMATCH",),
    ("marmalade", "verb", "(ngoại động từ) Phết mứt cam lên."): ("DICTIONARY_METADATA_IN_MEANING",),
    ("optimistic", "adjective", "Lạc quan chủ nghĩa."): ("POS_MEANING_MISMATCH",),
    ("unfriendly", "adjective", "Cừu địch."): ("VIETNAMESE_TYPO",),
    ("over", "noun", "Trên khắp, ở khắp."): ("POS_MEANING_MISMATCH",),
    ("busy", "verb", "Động từ phãn thân bận rộn với."): ("DICTIONARY_METADATA_IN_MEANING", "VIETNAMESE_TYPO"),
    ("thai", "noun", "Tiếng Thái."): ("HEADWORD_CASE_MISMATCH",),
    ("thai", "noun", "Người Thái."): ("HEADWORD_CASE_MISMATCH",),
    ("regulation", "noun", "Theo quy tắc, theo quy định, đúng phép, hợp lệ; thông thường, thường lệ."): ("POS_MEANING_MISMATCH",),
    ("monday", "noun", "Ngày thứ hai."): ("HEADWORD_CASE_MISMATCH",),
    ("reader", "noun", "Người đọc và sửa bản in thử ((cũng) proot reader)."): ("ENGLISH_TYPO", "DICTIONARY_METADATA_IN_MEANING"),
    ("trailroad", "verb", "Xây dựng đường sắt."): ("INVALID_OR_MISSPELLED_HEADWORD",),
    ("mean", "noun", "Phương tiện, kế, biện pháp, cách."): ("HEADWORD_NUMBER_OR_SENSE_MISMATCH",),
}


payload = json.loads(SOURCE.read_text(encoding="utf-8"))
rows = []
for row in payload["rows"]:
    key = (str(row["english"]).casefold(), str(row["part_of_speech"]).casefold(), str(row["vietnamese"]))
    reasons = list(FINDINGS.get(key, ()))
    rows.append({**row, "reviewVerdict": "NEEDS_REVIEW" if reasons else "PASS_IN_SAMPLE",
                 "reviewReasons": reasons})
verdicts = Counter(row["reviewVerdict"] for row in rows)
reasons = Counter(reason for row in rows for reason in row["reviewReasons"])
summary = {
    **payload["summary"],
    "reviewMethod": "Independent AI-assisted field-by-field semantic review; human-reviewed count: 0",
    "verdicts": dict(sorted(verdicts.items())),
    "reviewReasons": dict(sorted(reasons.items())),
    "qualityGate": "PASS" if verdicts.get("NEEDS_REVIEW", 0) == 0 else "FAIL",
}
JSON_OUT.write_text(json.dumps({"summary": summary, "rows": rows}, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8")
with CSV_OUT.open("w", encoding="utf-8-sig", newline="") as stream:
    fields = ["id", "topic", "part_of_speech", "level", "english", "vietnamese",
              "independent_meaning_confirmed", "reviewVerdict", "reviewReasons"]
    writer = csv.DictWriter(stream, fieldnames=fields)
    writer.writeheader()
    for row in rows:
        writer.writerow({**{key: row.get(key) for key in fields},
                         "reviewReasons": ";".join(row["reviewReasons"])})
print(json.dumps(summary, ensure_ascii=False, indent=2))
