"""Audit DataFix suggestions against VNEDICT without changing candidates or claiming human review."""
from __future__ import annotations

import csv
import gzip
import json
import re
import struct
import unicodedata
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OVERRIDES = ROOT / "datasets" / "quality" / "m15_5" / "curated_semantic_overrides.csv"
SOURCE = ROOT / "datasets" / "vnedict" / "source" / "compiled"
OUTPUT = ROOT / ".m15-5-verification" / "datafix-source-audit.json"


def norm(value: str) -> str:
    return " ".join(unicodedata.normalize("NFC", value or "").casefold().split()).strip(" .")


def load_vnedict() -> dict[str, set[str]]:
    dictionary = gzip.open(SOURCE / "vnedict.dict.dz", "rb").read()
    index = (SOURCE / "vnedict.idx").read_bytes()
    inverse: dict[str, set[str]] = defaultdict(set)
    position = 0
    while position < len(index):
        zero = index.index(0, position)
        vietnamese = norm(index[position:zero].decode("utf-8", "replace"))
        offset, size = struct.unpack(">II", index[zero + 1:zero + 9])
        gloss = dictionary[offset:offset + size].decode("utf-8", "replace").casefold()
        for token in set(re.findall(r"[a-z][a-z'-]*", gloss)):
            inverse[token].add(vietnamese)
        position = zero + 9
    return inverse


def main() -> None:
    source = load_vnedict()
    rows: list[dict[str, object]] = []
    with OVERRIDES.open(encoding="utf-8-sig", newline="") as stream:
        overrides = list(csv.DictReader(stream))
    for row in overrides:
        english = norm(row["english"])
        proposed = norm(row["suggested_vietnamese"])
        terms = sorted(source.get(english, set())) if " " not in english else []
        # A suggestion is source-confirmed only when every nonempty semicolon clause is an exact
        # VNEDICT headword. Substring overlap is evidence for review, never enough for replacement.
        clauses = [norm(part) for part in re.split(r"[;]", proposed) if norm(part)]
        exact = bool(clauses) and all(clause in terms for clause in clauses)
        overlap = sorted(term for term in terms if len(term) >= 3 and term in proposed)
        rows.append({
            "english": row["english"], "partOfSpeech": row["part_of_speech"],
            "originalVietnamese": row["original_vietnamese"],
            "proposedVietnamese": row["suggested_vietnamese"], "action": row["action"],
            "source": "VNEDICT_CC_BY_3_0", "exactSourceConfirmed": exact,
            "overlapEvidence": overlap, "humanVerified": False,
            "pipelineDisposition": "BLOCK_ORIGINAL_SELECT_DIFFERENT_CANDIDATE",
        })
    summary = {
        "rows": len(rows), "suggestReplace": sum(row["action"] == "SUGGEST_REPLACE" for row in rows),
        "blockReview": sum(row["action"] == "BLOCK_REVIEW" for row in rows),
        "exactSourceConfirmedSuggestions": sum(
            row["action"] == "SUGGEST_REPLACE" and row["exactSourceConfirmed"] for row in rows),
        "policy": "No proposal is written automatically; every exact triplet blocks the original candidate.",
        "humanReviewed": 0,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps({"summary": summary, "rows": rows}, ensure_ascii=False, indent=2) + "\n",
                      encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
