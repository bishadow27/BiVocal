"""Report VNEDICT lexical overlap; this is never semantic verification."""
from __future__ import annotations

import collections
import gzip
import json
import re
import struct
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "datasets" / "vnedict" / "source" / "compiled"
INPUT = ROOT / "datasets" / "processed" / "m15_5" / "candidates.jsonl"
OUTPUT = ROOT / "datasets" / "processed" / "m15_5" / "vnedict_cross_validation.jsonl"


def normalized(value: str) -> str:
    return unicodedata.normalize("NFC", value).casefold()


def load_index() -> dict[str, list[str]]:
    dictionary = gzip.open(SOURCE / "vnedict.dict.dz", "rb").read()
    index = (SOURCE / "vnedict.idx").read_bytes()
    result: dict[str, list[str]] = collections.defaultdict(list)
    position = 0
    while position < len(index):
        zero = index.index(0, position)
        vietnamese = normalized(index[position:zero].decode("utf-8", "replace"))
        offset, size = struct.unpack(">II", index[zero + 1:zero + 9])
        gloss = dictionary[offset:offset + size].decode("utf-8", "replace").casefold()
        for token in set(re.findall(r"[a-z][a-z'-]*", gloss)):
            result[token].append(vietnamese)
        position = zero + 9
    return result


inverse = load_index()
rows = [json.loads(line) for line in INPUT.open(encoding="utf-8")]
confirmed = []
for row in rows:
    english = str(row["normalizedEnglish"])
    meaning = normalized(str(row["vietnamese"]))
    matches = [] if " " in english else [term for term in inverse.get(english, []) if len(term) >= 2 and term in meaning]
    if matches:
        confirmed.append({"candidateId": row["candidateId"], "english": row["headword"],
                          "vietnamese": row["vietnamese"], "matchedVietnamese": max(matches, key=len),
                          "lexicalOverlap": True, "semanticVerified": False,
                          "matchMethod": "VNEDICT_VIETNAMESE_SUBSTRING_OVERLAP"})
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
with OUTPUT.open("w", encoding="utf-8", newline="\n") as stream:
    for row in confirmed:
        stream.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")
print(json.dumps({"input": len(rows), "lexicalOverlap": len(confirmed),
                  "semanticVerified": 0, "noLexicalOverlap": len(rows) - len(confirmed),
                  "warning": "Lexical substring overlap is not sense/POS/semantic verification.",
                  "output": str(OUTPUT)}, ensure_ascii=False, indent=2))
