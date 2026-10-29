"""Build the isolated M15.5 20k vocabulary candidate set from pinned local sources."""
from __future__ import annotations

import argparse
import collections
import csv
import json
import re
import sqlite3
from pathlib import Path
from typing import Iterable

import large_vocabulary_pipeline as base

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "backend" / "datasets" / "processed" / "m15_5"
VNEDICT_CONFIRMATION = OUTPUT / "vnedict_cross_validation.jsonl"
DATAFIX_OVERRIDES_PATH = ROOT / "backend" / "datasets" / "quality" / "m15_5" / "curated_semantic_overrides.csv"
REPAIR_V2_HARD_BLOCKS_PATH = ROOT / "backend" / "datasets" / "quality" / "m15_5" / "repair_v2" / "hard_block_only.csv"
GENERAL_TOPIC = {
    "code": "GENERAL_VOCABULARY", "name": "General Vocabulary",
    "description": "Từ vựng tổng quát chưa có đủ bằng chứng để xếp vào chủ đề chuyên biệt",
    "existing": False,
}
HEADWORD = re.compile(r"^[A-Za-z][A-Za-z' -]{0,79}$")
SEMANTIC_PATTERNS = {
    code: [(hint, re.compile(rf"(?<!\w){re.escape(base.normalize(hint))}(?!\w)")) for hint in hints]
    for code, hints in base.SEMANTIC_HINTS.items()
}
REFERENCE_MEANING = re.compile(
    r"^(?:xem\b|cách viết khác\b|số nhiều(?:\s+của)?\b|quá khứ\b|"
    r"động từ chia\b|viết tắt\b|\(?viết tắt\)?\s+của\b)", re.I)
UNSUITABLE_LEARNER_MEANING = re.compile(
    r"^(?:tên một họ\b|một tên dành cho\b)", re.I)
BAD_EXAMPLE_TEMPLATE = re.compile(
    r"\b(?:the entry .+ points to|they worked .+ all day|played a key role|"
    r"discussed the .+ today|helped everyone|learned to .+ the tool|"
    r"will .+ the files later)\b", re.I)
VIETNAMESE_NOUN_MARKER = re.compile(
    r"^(?:sự|việc|người|kẻ|cái|con|loài|giống|cây|chất|môn|ngành|lễ|chức|tính|độ|"
    r"trạng thái|tình trạng|bệnh|phép|thuật|học thuyết|chủ nghĩa|quan hệ|nơi|chỗ|vật)\b", re.I)
MALFORMED_MEANING = re.compile(r"(?:\b(?:cubic\s+c\.|xem\s+thêm)\b|\.{2,})", re.I)
LEAKED_ENGLISH_METADATA = re.compile(
    r"\b(?:the monarchy|in a different way|heart-free|zincography|cubic c\.)\b", re.I)
BROKEN_PHRASE_FRAGMENT = re.compile(r"^\s*\+\s*forth\)", re.I)
DICTIONARY_METADATA = re.compile(r"^\s*(?:ngoại động từ|nội động từ)\s*[.:]", re.I)
NOUN_GLOSSED_AS_ADJECTIVE = re.compile(r"^\s*\(thuộc\)", re.I)
KNOWN_CORRUPT_PATTERN = re.compile(
    r"^\s*[,.+]|\bnhân th\.|\bcổ tay áo\b|\bthuốc hủy diệt sinh thái\b", re.I)
DICTIONARY_USAGE_METADATA = re.compile(r"\(\(?viết tắt\)?|\(\(?nghĩa đen\)?|\(vt\.", re.I)
CEFR_LEVEL_RANK = {"A1": 0, "A2": 1, "B1": 2, "B2": 3}


def load_datafix_overrides(path: Path = DATAFIX_OVERRIDES_PATH) -> dict[tuple[str, str, str], dict[str, str]]:
    """Load suggestions as block evidence only; never claim human approval or rewrite a meaning."""
    if not path.exists():
        raise RuntimeError(f"Missing reviewed DataFix file: {path}")
    result: dict[tuple[str, str, str], dict[str, str]] = {}
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            key = (base.normalize(row["english"]), base.normalize(row["part_of_speech"]),
                   base.normalize(row["original_vietnamese"]))
            if key in result and result[key].get("suggested_vietnamese") != row.get("suggested_vietnamese"):
                raise RuntimeError(f"Conflicting DataFix overrides for {key!r}")
            result[key] = row
    return result


DATAFIX_OVERRIDES = load_datafix_overrides()


def load_repair_v2_hard_blocks(path: Path = REPAIR_V2_HARD_BLOCKS_PATH) -> dict[tuple[str, str, str], dict[str, str]]:
    """Load exact semantic blocks by English + POS + original Vietnamese, never staging ID."""
    if not path.exists():
        raise RuntimeError(f"Missing Repair v2 hard-block file: {path}")
    result: dict[tuple[str, str, str], dict[str, str]] = {}
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            key = (base.normalize(row["english"]), base.normalize(row["pos"]),
                   base.normalize(row["vietnamese"]))
            result[key] = row
    return result


REPAIR_V2_HARD_BLOCKS = load_repair_v2_hard_blocks()


def contains_headword(headword: str, example: str) -> bool:
    return bool(re.search(rf"(?<![A-Za-z]){re.escape(headword)}(?:s|es|ed|ing)?(?![A-Za-z])", example, re.I))


def classify_topic(headword: str, translation: str, source_codes: list[str]) -> tuple[list[str], str]:
    # A translated keyword is not enough evidence for topic assignment (for example
    # "con" in a playing-card sense or "thuốc cặn" falsely mapping to HEALTH).
    # Only the pinned CEFR category may assign a specialist topic automatically.
    if source_codes: return source_codes, "SOURCE_CEFR_CATEGORY"
    return [GENERAL_TOPIC["code"]], "GENERAL_DICTIONARY_TOPIC"


def lexical_key(value: str) -> str:
    return re.sub(r"[^a-zà-ỹđ]+", "", base.normalize(value))


def structural_reasons(candidate: dict[str, object]) -> list[str]:
    word = str(candidate["headword"]); normalized = str(candidate["normalizedEnglish"])
    meaning = str(candidate["vietnamese"]); pos = str(candidate["partOfSpeech"])
    reasons: list[str] = []
    if not HEADWORD.fullmatch(word) or len(word.split()) > 5:
        reasons.append("INVALID_LEARNER_HEADWORD")
    if candidate["partOfSpeech"] not in base.SUPPORTED_POS:
        reasons.append("UNSUPPORTED_PART_OF_SPEECH")
    if not 1 <= len(meaning) <= 240 or not re.search(r"[A-Za-zÀ-ỹĐđ]", meaning):
        reasons.append("INVALID_VIETNAMESE_LENGTH_OR_TEXT")
    if base.MARKUP.search(meaning): reasons.append("UNSAFE_TRANSLATION_MARKUP")
    if REFERENCE_MEANING.search(meaning): reasons.append("REFERENCE_ONLY_MEANING")
    if UNSUITABLE_LEARNER_MEANING.search(meaning): reasons.append("UNSUITABLE_LEARNER_MEANING")
    if base.USAGE_LABEL.search(meaning): reasons.append("NON_CORE_USAGE_LABEL")
    if base.BLOCKED_CONTENT.search(f"{word} {meaning}"): reasons.append("INAPPROPRIATE_CONTENT")
    if normalized == base.normalize(meaning) or lexical_key(normalized) == lexical_key(meaning):
        reasons.append("TRANSLATION_EQUALS_HEADWORD")
    if MALFORMED_MEANING.search(meaning): reasons.append("MALFORMED_DICTIONARY_DEFINITION")
    if LEAKED_ENGLISH_METADATA.search(meaning): reasons.append("LEAKED_ENGLISH_METADATA")
    if BROKEN_PHRASE_FRAGMENT.search(meaning): reasons.append("BROKEN_PHRASE_FRAGMENT")
    if DICTIONARY_METADATA.search(meaning): reasons.append("DICTIONARY_METADATA")
    if pos == "noun" and NOUN_GLOSSED_AS_ADJECTIVE.search(meaning):
        reasons.append("NOUN_GLOSSED_AS_ADJECTIVE")
    if KNOWN_CORRUPT_PATTERN.search(meaning): reasons.append("KNOWN_CORRUPT_PATTERN")
    if DICTIONARY_USAGE_METADATA.search(meaning): reasons.append("DICTIONARY_USAGE_METADATA")
    if pos in {"adjective", "adverb", "verb"} and VIETNAMESE_NOUN_MARKER.search(meaning):
        reasons.append("PART_OF_SPEECH_MEANING_CONFLICT")
    datafix_key = (base.normalize(word), base.normalize(pos), base.normalize(meaning))
    if datafix_key in DATAFIX_OVERRIDES:
        reasons.append("DATAFIX_KNOWN_SEMANTIC_ISSUE")
    if datafix_key in REPAIR_V2_HARD_BLOCKS:
        reasons.append("REPAIR_V2_HARD_BLOCK")
    if not candidate["primaryTopicCode"]: reasons.append("TOPIC_UNCLASSIFIED")
    if candidate["cefrStatus"] == "conflict": reasons.append("SOURCE_CEFR_CONFLICT")
    return sorted(set(reasons))


def write_jsonl(path: Path, rows: Iterable[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        for row in rows:
            stream.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")


def choose(rows: list[dict[str, object]], limit: int) -> list[dict[str, object]]:
    """Use independently confirmed meanings first, then CEFR-backed core senses."""
    semantically_verified = [row for row in rows if row["source"].get("semanticVerified") is True]  # type: ignore[union-attr]
    confirmed_ordered = sorted(semantically_verified, key=lambda x: (
        CEFR_LEVEL_RANK.get(str(x["cefr"]), 8),
        int(x["source"]["wordDefinitionId"]),  # type: ignore[index]
        -int(x["qualityScore"]), str(x["candidateId"])))
    verified = [row for row in rows if row["cefrStatus"] == "exact"
                and row["source"].get("semanticVerified") is not True]  # type: ignore[union-attr]
    verified_ordered = sorted(verified, key=lambda x: (
        CEFR_LEVEL_RANK.get(str(x["cefr"]), 9),
        int(x["source"]["wordDefinitionId"]),  # type: ignore[index]
        -int(x["qualityScore"]), str(x["candidateId"])))
    supplemental = [row for row in rows if row["cefrStatus"] == "undetermined"
        and "-" not in str(row["headword"]) and " " not in str(row["headword"])
        and len(str(row["headword"])) <= 14 and len(str(row["vietnamese"])) <= 100
        and ";" not in str(row["vietnamese"])
        and row["source"]["dictionaryPos"] in {"N", "V", "A", "D", "E", "P", "C"}]  # type: ignore[index]
    supplemental_ordered = sorted(supplemental, key=lambda x: (
        int(x["source"]["wordDefinitionId"]),  # type: ignore[index]
        -int(x["qualityScore"]), str(x["candidateId"])))
    selected: list[dict[str, object]] = []
    selected_ids: set[str] = set()
    counts: collections.Counter[tuple[str, str]] = collections.Counter()
    meanings: set[tuple[str, str, str]] = set()
    for pass_number in (1, 2, 3):
        for row in confirmed_ordered:
            if len(selected) >= limit: return selected
            cid = str(row["candidateId"]); key = (str(row["normalizedEnglish"]), str(row["partOfSpeech"]))
            sense = (str(row["normalizedEnglish"]), "", base.normalize(str(row["vietnamese"])))
            if cid in selected_ids or counts[key] >= pass_number or sense in meanings: continue
            selected.append(row); selected_ids.add(cid); counts[key] += 1; meanings.add(sense)
    for pass_number in (1, 2, 3):
        for row in verified_ordered:
            if len(selected) >= limit: return selected
            cid = str(row["candidateId"]); key = (str(row["normalizedEnglish"]), str(row["partOfSpeech"]))
            sense = (str(row["normalizedEnglish"]), "", base.normalize(str(row["vietnamese"])))
            if cid in selected_ids or counts[key] >= pass_number or sense in meanings: continue
            selected.append(row); selected_ids.add(cid); counts[key] += 1; meanings.add(sense)
    for row in supplemental_ordered:
        if len(selected) >= limit: return selected
        cid = str(row["candidateId"]); key = (str(row["normalizedEnglish"]), str(row["partOfSpeech"]))
        sense = (str(row["normalizedEnglish"]), "", base.normalize(str(row["vietnamese"])))
        if cid in selected_ids or counts[key] or sense in meanings: continue
        selected.append(row); selected_ids.add(cid); counts[key] = 1; meanings.add(sense)
    return selected


def build(approved_limit: int = 22000) -> dict[str, object]:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if not VNEDICT_CONFIRMATION.exists():
        raise RuntimeError("Run cross_validate_vnedict.py before the final M15.5 build")
    confirmed_rows = [json.loads(line) for line in VNEDICT_CONFIRMATION.open(encoding="utf-8")]
    lexical_overlaps = {str(row["candidateId"]): str(row["matchedVietnamese"]) for row in confirmed_rows}
    semantic_confirmations = {str(row["candidateId"]) for row in confirmed_rows
                              if row.get("semanticVerified") is True}
    with base.CEFR_PATH.open(encoding="utf-8-sig", newline="") as stream:
        cefr_rows = list(csv.DictReader(stream, delimiter=";"))
    cefr: dict[tuple[str, str], dict[str, object]] = {}
    grouped: dict[tuple[str, str], list[dict[str, str]]] = collections.defaultdict(list)
    for row in cefr_rows: grouped[(base.normalize(row["headword"]), base.normalize(row["pos"]))].append(row)
    for key, rows in grouped.items():
        levels = sorted({base.clean(row["CEFR"]).upper() for row in rows})
        labels, topic_codes = base.source_topics(rows)
        cefr[key] = {"levels": levels, "labels": labels, "topics": topic_codes}

    con = sqlite3.connect(f"file:{base.DICTIONARY_PATH.as_posix()}?mode=ro", uri=True)
    con.row_factory = sqlite3.Row
    if con.execute("PRAGMA integrity_check").fetchone()[0] != "ok": raise RuntimeError("SQLite integrity_check failed")
    pronunciations: dict[int, list[dict[str, object]]] = collections.defaultdict(list)
    for row in con.execute("SELECT word_id,id,ipa,region FROM pronunciations ORDER BY word_id,id"):
        pronunciations[int(row["word_id"])].append(dict(row))

    candidates: list[dict[str, object]] = []
    early_rejected_samples: list[dict[str, object]] = []
    early_rejected_reasons: collections.Counter[str] = collections.Counter()
    source_rows = 0
    query = """SELECT w.id word_id,w.word,wd.id link_id,d.id definition_id,d.definition,d.pos,d.sub_pos,wd.example
      FROM words w JOIN word_definitions wd ON wd.word_id=w.id JOIN definitions d ON d.id=wd.definition_id
      ORDER BY w.id,wd.id"""
    for row in con.execute(query):
        source_rows += 1
        canonical = base.POS_MAP.get(base.clean(row["pos"])); word = base.normalize(row["word"])
        group = cefr.get((word, canonical or ""), {})
        levels = list(group.get("levels", [])); source_topics = list(group.get("topics", []))
        labels = list(group.get("labels", [])); ipa, variants = base.choose_ipa(pronunciations[int(row["word_id"])])
        source_example = base.clean(row["example"])
        mapped, method = classify_topic(base.clean(row["word"]), base.clean(row["definition"]), source_topics)
        cefr_status = "exact" if len(levels) == 1 else "conflict" if len(levels) > 1 else "undetermined"
        candidate_id = base.stable_id(row["word_id"], row["definition_id"], row["link_id"], canonical)
        candidate: dict[str, object] = {
            "schemaVersion": 2,
            "candidateId": candidate_id,
            "normalizedKey": "\u0000".join((word, canonical or "", base.normalize(row["definition"]))),
            "headword": base.clean(row["word"]), "normalizedEnglish": word,
            "partOfSpeech": canonical, "cefr": levels[0] if len(levels) == 1 else None,
            "cefrStatus": cefr_status,
            "cefrStatusReason": "EXACT_HEADWORD_POS" if cefr_status == "exact" else
                "SOURCE_CEFR_CONFLICT" if cefr_status == "conflict" else "NOT_PRESENT_IN_PINNED_CEFR_SOURCE",
            "cefrScope": "HEADWORD_POS_NOT_SENSE" if cefr_status == "exact" else "UNDETERMINED",
            "vietnamese": base.clean(row["definition"]), "pronunciation": None,
            "pronunciationVariants": [], "example": None, "exampleVi": None,
            "sourceTopics": labels, "topicCodes": mapped, "primaryTopicCode": mapped[0],
            "topicClassificationMethod": method, "senseEvidence": "SQLITE_WORD_DEFINITION_LINK",
            "source": {"name": "Skypedia English-Vietnamese Dictionary", "version": base.SOURCE_COMMIT,
                "url": base.SOURCE_URL, "license": base.SOURCE_LICENSE, "wordId": int(row["word_id"]),
                "definitionId": int(row["definition_id"]), "wordDefinitionId": int(row["link_id"]),
                "dictionaryPos": row["pos"], "subPos": row["sub_pos"],
                "unverifiedPronunciation": ipa, "unverifiedPronunciationVariants": variants,
                "unverifiedExample": source_example,
                # Compatibility field remains false: lexical substring overlap is not meaning confirmation.
                "independentMeaningConfirmed": candidate_id in semantic_confirmations,
                "independentMeaningSource": "VNEDICT_CC_BY_3_0" if candidate_id in semantic_confirmations else None,
                "independentMeaningMatch": lexical_overlaps.get(candidate_id) if candidate_id in semantic_confirmations else None,
                "independentLexicalOverlap": candidate_id in lexical_overlaps,
                "independentLexicalOverlapSource": "VNEDICT_CC_BY_3_0" if candidate_id in lexical_overlaps else None,
                "independentLexicalOverlapMatch": lexical_overlaps.get(candidate_id),
                "semanticVerified": candidate_id in semantic_confirmations,
                "semanticVerificationMethod": None},
            "validationStatus": "NEEDS_REVIEW", "reviewClassification": "NEEDS_MANUAL_REVIEW",
            "validationReasons": [], "importEligible": False, "quizEligible": False,
        }
        datafix = DATAFIX_OVERRIDES.get((word, base.normalize(canonical or ""), base.normalize(row["definition"])))
        if datafix:
            candidate["source"]["dataFix"] = {  # type: ignore[index]
                "action": datafix["action"], "reason": datafix["reason"],
                "suggestedVietnamese": datafix["suggested_vietnamese"],
                "reviewStatus": "NOT_HUMAN_VERIFIED", "appliedAs": "BLOCK_ORIGINAL",
            }
        repair_v2 = REPAIR_V2_HARD_BLOCKS.get(
            (word, base.normalize(canonical or ""), base.normalize(row["definition"])))
        if repair_v2:
            candidate["source"]["repairV2"] = {  # type: ignore[index]
                "decision": "BLOCK", "severity": repair_v2["severity"],
                "riskRules": repair_v2["risk_rules"], "reviewStatus": "NOT_HUMAN_VERIFIED",
                "matchMethod": "ENGLISH_POS_ORIGINAL_VIETNAMESE",
            }
        candidate["validationReasons"] = structural_reasons(candidate)
        score_input = {**candidate, "pronunciation": ipa, "example": source_example}
        candidate["qualityScore"] = base.candidate_score(score_input) + (20 if cefr_status == "exact" else 0)
        if candidate["validationReasons"]:
            candidate["validationStatus"] = "REJECTED"; candidate["reviewClassification"] = "REJECTED"
            early_rejected_reasons.update(candidate["validationReasons"])
            if len(early_rejected_samples) < 1000: early_rejected_samples.append(candidate)
            continue
        candidates.append(candidate)
    con.close()

    groups: dict[str, list[dict[str, object]]] = collections.defaultdict(list)
    for row in candidates: groups[str(row["normalizedKey"])].append(row)
    for group in groups.values():
        for duplicate in sorted(group, key=lambda x: str(x["candidateId"]))[1:]:
            duplicate["validationReasons"] = sorted(set(list(duplicate["validationReasons"]) + ["DUPLICATE_NORMALIZED_SENSE"]))

    # PostgreSQL uniqueness is English + Vietnamese regardless of POS. Keep the deterministic
    # first candidate only so the full artifact cannot hide a cross-POS duplicate behind IDs.
    global_senses: dict[tuple[str, str], list[dict[str, object]]] = collections.defaultdict(list)
    for row in candidates:
        global_senses[(str(row["normalizedEnglish"]), base.normalize(str(row["vietnamese"])))].append(row)
    for group in global_senses.values():
        for duplicate in sorted(group, key=lambda x: str(x["candidateId"]))[1:]:
            duplicate["validationReasons"] = sorted(set(
                list(duplicate["validationReasons"]) + ["DUPLICATE_EN_VI_ACROSS_POS"]))

    near_duplicates = base.near_duplicate_losers(candidates)
    hard = base.HARD_REJECT_REASONS | {"UNSUPPORTED_PART_OF_SPEECH"}
    eligible: list[dict[str, object]] = []
    for row in candidates:
        reasons = set(row["validationReasons"])
        if str(row["candidateId"]) in near_duplicates:
            reasons.add("POSSIBLE_DUPLICATE_MEANING"); row["validationReasons"] = sorted(reasons)
        if reasons & {"DUPLICATE_NORMALIZED_SENSE", "DUPLICATE_EN_VI_ACROSS_POS"}:
            row["validationStatus"] = "REJECTED"; row["reviewClassification"] = "DUPLICATE_EXISTING"
        elif reasons & hard:
            row["validationStatus"] = "REJECTED"; row["reviewClassification"] = "REJECTED"
        elif "POSSIBLE_DUPLICATE_MEANING" in reasons:
            row["validationStatus"] = "NEEDS_REVIEW"; row["reviewClassification"] = "NEEDS_MANUAL_REVIEW"
        else: eligible.append(row)

    selected = choose(eligible, approved_limit); selected_ids = {str(x["candidateId"]) for x in selected}
    if len(selected) < approved_limit:
        raise RuntimeError(f"Only {len(selected)} candidates passed the quality gate; {approved_limit} required")
    word_pos_counts = collections.Counter((str(x["normalizedEnglish"]), str(x["partOfSpeech"])) for x in selected)
    answer_counts = collections.Counter((str(x["primaryTopicCode"]), base.normalize(str(x["vietnamese"]))) for x in selected)
    for row in eligible:
        if str(row["candidateId"]) in selected_ids:
            row["validationStatus"] = "APPROVED_AUTO"; row["reviewClassification"] = "READY_TO_APPROVE"
            row["importEligible"] = True
            key = (str(row["normalizedEnglish"]), str(row["partOfSpeech"]))
            answer = (str(row["primaryTopicCode"]), base.normalize(str(row["vietnamese"])))
            row["quizEligible"] = word_pos_counts[key] == 1 and answer_counts[answer] == 1
        else:
            row["validationStatus"] = "NEEDS_REVIEW"; row["reviewClassification"] = "NEEDS_MANUAL_REVIEW"
            row["validationReasons"] = sorted(set(list(row["validationReasons"]) + ["DEFERRED_TARGET_CAPACITY"]))

    candidates.sort(key=lambda x: (str(x["validationStatus"]), str(x["candidateId"])))
    approved = [x for x in candidates if x["validationStatus"] == "APPROVED_AUTO"]
    review = [x for x in candidates if x["validationStatus"] == "NEEDS_REVIEW"]
    rejected = [x for x in candidates if x["validationStatus"] == "REJECTED"] + early_rejected_samples
    reasons = collections.Counter(early_rejected_reasons)
    reasons.update(r for x in candidates for r in x["validationReasons"])
    classifications = collections.Counter(str(x["reviewClassification"]) for x in candidates)
    classifications["REJECTED"] += source_rows - len(candidates)
    levels = collections.Counter(str(x["cefr"] or "UNDETERMINED") for x in approved)
    topics = collections.Counter(str(x["primaryTopicCode"]) for x in approved)
    stats = {"targetTotalFlashcards": 20000, "productionBaseline": 5000,
        "approvedCapacity": len(approved), "sourceRows": source_rows, "materializedCandidates": len(candidates),
        "rejectedRows": source_rows-len(candidates)+sum(x["validationStatus"] == "REJECTED" for x in candidates),
        "rejectedArtifactSamples": len(rejected),
        "status": {"APPROVED_AUTO": len(approved), "NEEDS_REVIEW": len(review),
          "REJECTED": source_rows-len(candidates)+sum(x["validationStatus"] == "REJECTED" for x in candidates)},
        "classifications": dict(sorted(classifications.items())), "approvedLevels": dict(sorted(levels.items())),
        "approvedTopics": dict(sorted(topics.items())), "approvedQuizEligible": sum(bool(x["quizEligible"]) for x in approved),
        "approvedDistinctHeadwords": len({str(x["normalizedEnglish"]) for x in approved}),
        "validationReasons": dict(sorted(reasons.items())),
        "source": {"dictionaryLicense": base.SOURCE_LICENSE, "dictionaryCommit": base.SOURCE_COMMIT,
          "dictionarySha256": base.sha256_file(base.DICTIONARY_PATH), "cefrLicense": "MIT",
          "cefrRows": len(cefr_rows), "unknownCefrPolicy": "NULL_NOT_INFERRED",
          "dataFixSha256": base.sha256_file(DATAFIX_OVERRIDES_PATH),
          "dataFixRows": len(DATAFIX_OVERRIDES),
          "dataFixPolicy": "EXACT_ENGLISH_POS_ORIGINAL_MEANING_BLOCK_ONLY_NOT_HUMAN_VERIFIED",
          "repairV2Sha256": base.sha256_file(REPAIR_V2_HARD_BLOCKS_PATH),
          "repairV2HardBlockRows": len(REPAIR_V2_HARD_BLOCKS),
          "vnedictPolicy": "LEXICAL_SUBSTRING_OVERLAP_ONLY_SEMANTIC_VERIFIED_FALSE"}}
    write_jsonl(OUTPUT / "candidates.jsonl", candidates); write_jsonl(OUTPUT / "approved.jsonl", approved)
    write_jsonl(OUTPUT / "review_queue.jsonl", review); write_jsonl(OUTPUT / "rejected.jsonl", rejected)
    topic_rows = [{"code": code, "name": v[0], "description": v[1], "existing": v[2]} for code, v in base.TOPICS.items()]
    topic_rows.append(GENERAL_TOPIC)
    (OUTPUT / "topics.json").write_text(json.dumps(topic_rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUTPUT / "statistics.json").write_text(json.dumps(stats, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return stats


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--approved-capacity", type=int, default=22000)
    args = parser.parse_args()
    if not 15000 <= args.approved_capacity <= 50000: parser.error("capacity must be 15000..50000")
    print(json.dumps(build(args.approved_capacity), ensure_ascii=False, indent=2))
