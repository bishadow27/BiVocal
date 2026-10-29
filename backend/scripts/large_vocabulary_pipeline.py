"""Build reproducible BiVoca vocabulary batches from CEFR CSV and the source SQLite dictionary."""
from __future__ import annotations

import argparse
import collections
import csv
import hashlib
import json
import re
import sqlite3
import unicodedata
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[2]
CEFR_PATH = ROOT / "backend" / "datasets" / "cefr" / "word_list_cefr.csv"
DICTIONARY_PATH = ROOT / "backend" / "datasets" / "en-vi" / "dictionary_en_vi.db"
EXISTING_PATH = ROOT / "dataset" / "flashcards.csv"
OUTPUT_DIR = ROOT / "backend" / "datasets" / "processed"
SOURCE_COMMIT = "388adc0826300912e5b0311c089e0e70494b065f"
SOURCE_URL = "https://github.com/skypediacode/english-vietnamese-dictionary"
SOURCE_LICENSE = "CC BY-SA 4.0"

POS_MAP = {
    "N": "noun", "noun": "noun", "n": "noun",
    "V": "verb", "verb": "verb",
    "A": "adjective", "adj": "adjective",
    "D": "adverb", "adv": "adverb",
    "E": "preposition", "P": "pronoun", "pron": "pronoun", "C": "conjunction",
}
SUPPORTED_POS = frozenset(POS_MAP.values())

TOPICS = {
    "FOOD": ("Food", "Từ vựng về thức ăn và đồ uống", True),
    "ANIMALS": ("Animals", "Từ vựng về động vật và môi trường sống", True),
    "TRAVEL": ("Travel", "Từ vựng dùng khi đi du lịch", True),
    "SCHOOL": ("School", "Từ vựng về trường học và học tập", True),
    "FAMILY": ("Family", "Từ vựng về gia đình và các mối quan hệ", True),
    "TECHNOLOGY": ("Technology", "Từ vựng về công nghệ và thiết bị số", True),
    "WORK": ("Work", "Từ vựng về công việc và môi trường làm việc", True),
    "HEALTH": ("Health", "Từ vựng về sức khỏe và chăm sóc cơ thể", True),
    "SHOPPING": ("Shopping", "Từ vựng về mua sắm và thanh toán", True),
    "WEATHER": ("Weather", "Từ vựng về thời tiết và khí hậu", True),
    "PERSONAL": ("Personal Life", "Thông tin cá nhân và đời sống", False),
    "LEISURE": ("Leisure", "Giải trí, sở thích và hoạt động lúc rảnh", False),
    "HOME": ("House & Home", "Nhà ở, phòng và đồ dùng gia đình", False),
    "DAILY_LIFE": ("Daily Life", "Sinh hoạt và dịch vụ hằng ngày", False),
    "TRANSPORTATION": ("Transportation", "Phương tiện và cách di chuyển", False),
    "RELATIONSHIPS": ("Relationships", "Quan hệ và giao tiếp xã hội", False),
    "CULTURE": ("Culture", "Văn hóa, nghệ thuật và quốc gia", False),
    "SCIENCE": ("Science", "Khoa học và phát triển", False),
    "EMOTIONS": ("Emotions", "Cảm xúc và đặc điểm tính cách", False),
}

CATEGORY_TOPIC = {
    "Personal identification": "PERSONAL", "Personal information": "PERSONAL",
    "Education": "SCHOOL", "Language": "SCHOOL", "Books and literature": "SCHOOL",
    "Food and drink": "FOOD", "Free time, entertainment": "LEISURE",
    "Hobbies and pastimes": "LEISURE", "Leisure activities": "LEISURE",
    "Film": "LEISURE", "Hobbies and lifestyles": "LEISURE", "Arts": "CULTURE",
    "Work and jobs": "WORK", "Shopping": "SHOPPING",
    "Things in the town, shops and shopping": "SHOPPING", "Clothes": "SHOPPING",
    "Objects and rooms": "HOME", "House and home, environment": "HOME",
    "News, lifestyles and current affairs": "DAILY_LIFE", "Daily life": "DAILY_LIFE",
    "Services": "DAILY_LIFE", "Travel": "TRAVEL", "Travel and services vocab": "TRAVEL",
    "Ways of travelling": "TRANSPORTATION", "Holidays": "TRAVEL", "Places": "TRAVEL",
    "Health and body care": "HEALTH", "Relations with other people": "RELATIONSHIPS",
    "Family life": "FAMILY", "Nationalities and countries": "CULTURE",
    "Media": "TECHNOLOGY", "Weather": "WEATHER", "Scientific development": "SCIENCE",
    "Colours": "DAILY_LIFE", "Dimensions": "DAILY_LIFE",
    "Adjectives: personality, description, feelings": "EMOTIONS",
}

USAGE_LABEL = re.compile(
    r"\((?:từ lóng|thông tục|tục|nghĩa cổ|cổ|thơ ca|hiếm|không dùng|lỗi thời|"
    r"địa phương|phương ngữ|như)\b", re.I)
BLOCKED_CONTENT = re.compile(
    r"\b(?:fuck|shit|cunt|porn|penis|vagina|dương vật|âm hộ|giao cấu)\b", re.I)
GENERIC_EXAMPLE = re.compile(
    r"(?:\b(the word|the phrase|the prefix|the suffix|the old form)\b.*"
    r"\b(used in english|common long ago|appears in|texts?)|"
    r"\b(?:was|were) (?:useful|important) (?:in class|at work)\b|"
    r"\bhelped the team\b|\bappeared in the report\b)", re.I)
MARKUP = re.compile(r"<[^>]+>|https?://|\{\{|\[\[|\|lang=", re.I)
MEANING_STOP_WORDS = frozenset({"từ", "ở", "là", "một", "các", "của", "về", "sự", "việc", "cái", "con"})
HARD_REJECT_REASONS = frozenset({
    "INVALID_LEARNER_HEADWORD", "INVALID_VIETNAMESE_LENGTH_OR_TEXT",
    "TRANSLATION_EQUALS_HEADWORD", "UNSAFE_TRANSLATION_MARKUP", "INAPPROPRIATE_CONTENT",
    "INVALID_EXAMPLE_LENGTH", "EXAMPLE_MISSING_HEADWORD",
    "GENERIC_OR_PLACEHOLDER_EXAMPLE", "DUPLICATE_NORMALIZED_SENSE",
    "MISSING_VALID_IPA", "MALFORMED_DICTIONARY_DEFINITION",
    "NON_CORE_USAGE_LABEL", "SOURCE_CEFR_CONFLICT", "TOPIC_UNCLASSIFIED",
    "EXISTING_FLASHCARD_PRESERVED",
})

SEMANTIC_HINTS = {
    "FOOD": ("thức ăn", "đồ ăn", "đồ uống", "ăn", "uống", "bánh", "thịt", "rau", "trái cây", "nấu"),
    "ANIMALS": ("động vật", "con vật", "loài vật", "chim", "thú", "côn trùng", "vật nuôi"),
    "TRAVEL": ("du lịch", "khách sạn", "hành lý", "hộ chiếu", "chuyến đi", "sân bay", "kỳ nghỉ"),
    "SCHOOL": ("giáo dục", "trường học", "học sinh", "sinh viên", "giáo viên", "bài thi", "ngữ pháp", "ngôn ngữ"),
    "FAMILY": ("gia đình", "cha", "mẹ", "anh chị em", "họ hàng", "vợ", "chồng", "con trai", "con gái"),
    "TECHNOLOGY": ("công nghệ", "máy tính", "internet", "điện thoại", "phần mềm", "dữ liệu", "kỹ thuật số"),
    "WORK": ("công việc", "nghề nghiệp", "nhân viên", "công ty", "doanh nghiệp", "lao động", "việc làm"),
    "HEALTH": ("sức khỏe", "bệnh", "thuốc", "chữa", "điều trị", "cơ thể", "y tế", "bác sĩ", "đau"),
    "SHOPPING": ("mua", "bán", "giá tiền", "cửa hàng", "thanh toán", "hàng hóa", "quần áo"),
    "WEATHER": ("thời tiết", "mưa", "nắng", "gió", "bão", "khí hậu", "nhiệt độ"),
    "PERSONAL": ("thông tin cá nhân", "danh tính", "tên", "tuổi", "địa chỉ"),
    "LEISURE": ("giải trí", "trò chơi", "thể thao", "âm nhạc", "phim", "sở thích"),
    "HOME": ("nhà ở", "ngôi nhà", "căn nhà", "căn phòng", "phòng ngủ", "đồ đạc", "nhà bếp", "giường"),
    "DAILY_LIFE": ("hằng ngày", "hàng ngày", "sinh hoạt", "dịch vụ", "cuộc sống"),
    "TRANSPORTATION": ("phương tiện", "vận chuyển", "xe buýt", "xe hơi", "ô tô", "tàu hỏa", "đường sắt"),
    "RELATIONSHIPS": ("mối quan hệ", "giao tiếp", "tình bạn", "bạn bè", "người yêu"),
    "CULTURE": ("văn hóa", "nghệ thuật", "dân tộc", "truyền thống", "quốc gia"),
    "SCIENCE": ("khoa học", "nghiên cứu", "thí nghiệm", "nhà khoa học"),
    "EMOTIONS": ("cảm xúc", "vui", "buồn", "tức giận", "sợ hãi", "tính cách"),
}


def normalize(value: str | None) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKC", (value or "").strip())).casefold()


def clean(value: str | None) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", (value or "").strip()))


def meaning_tokens(value: str) -> set[str]:
    """Return conservative Vietnamese content tokens for near-duplicate review."""
    return {token for token in re.findall(r"[a-zà-ỹđ]+", clean(value).casefold())
            if token not in MEANING_STOP_WORDS}


def near_duplicate_losers(candidates: list[dict[str, object]]) -> set[str]:
    """Flag only high-overlap meanings; uncertain pairs go to review, never auto-publish."""
    groups: dict[tuple[str, str], list[dict[str, object]]] = collections.defaultdict(list)
    for candidate in candidates:
        if set(candidate["validationReasons"]) & HARD_REJECT_REASONS:  # type: ignore[arg-type]
            continue
        groups[(str(candidate["normalizedEnglish"]), str(candidate["partOfSpeech"]))].append(candidate)
    losers: set[str] = set()
    for group in groups.values():
        for index, left in enumerate(group):
            left_tokens = meaning_tokens(str(left["vietnamese"]))
            for right in group[index + 1:]:
                right_tokens = meaning_tokens(str(right["vietnamese"]))
                if not left_tokens or not right_tokens:
                    continue
                overlap = len(left_tokens & right_tokens)
                jaccard = overlap / len(left_tokens | right_tokens)
                if overlap < 2 or jaccard < .85:
                    continue
                ranked = sorted((left, right), key=lambda item: (
                    -int(item["qualityScore"]), len(meaning_tokens(str(item["vietnamese"]))),
                    str(item["candidateId"])))
                losers.add(str(ranked[1]["candidateId"]))
    return losers


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def stable_id(*parts: object) -> str:
    raw = "\0".join(str(part) for part in parts)
    return "bivoca-envi-" + hashlib.sha256(raw.encode("utf-8")).hexdigest()[:24]


def source_topics(rows: list[dict[str, str]]) -> tuple[list[str], list[str]]:
    labels: list[str] = []
    codes: list[str] = []
    for row in rows:
        for field in ("CoreInventory 1", "CoreInventory 2", "Threshold"):
            label = clean(row.get(field))
            if label and label not in labels:
                labels.append(label)
            code = CATEGORY_TOPIC.get(label)
            if code and code not in codes:
                codes.append(code)
    return labels, codes


def classify_topic(headword: str, translation: str, source_codes: list[str]) -> tuple[list[str], str]:
    text = normalize(f"{headword} {translation}")
    scores: dict[str, int] = {}
    for code, hints in SEMANTIC_HINTS.items():
        score = sum(1 for hint in hints if re.search(rf"(?<!\w){re.escape(normalize(hint))}(?!\w)", text))
        if score:
            scores[code] = score
    if scores:
        maximum = max(scores.values())
        best = sorted(code for code, score in scores.items() if score == maximum)
        source_best = [code for code in source_codes if code in best]
        if source_best:
            return source_best + [code for code in source_codes if code not in source_best], "MEANING_AND_SOURCE_CONFIRMED"
        return [], "MEANING_SOURCE_TOPIC_CONFLICT"
    if source_codes:
        # CEFR source categories are retained evidence. When the Vietnamese definition
        # has no stronger semantic hint, keep their stable source order and use the
        # first mapped category as primary instead of inventing a topic.
        return source_codes, "SOURCE_CEFR_CATEGORY"
    return [], "TOPIC_AMBIGUOUS_OR_UNCLASSIFIED"


def choose_ipa(variants: list[dict[str, object]]) -> tuple[str | None, list[dict[str, object]]]:
    valid = [item for item in variants if 2 <= len(clean(str(item["ipa"]))) <= 100
             and clean(str(item["ipa"])).startswith(("/", "["))]
    priority = {"BrE": 0, "UK": 1, "uk": 2, None: 3, "NAmE": 4,
                "General-American": 5, "US": 6}
    valid.sort(key=lambda item: (priority.get(item["region"], 20), int(item["id"])))
    return (clean(str(valid[0]["ipa"])) if valid else None, valid)


def quality_reasons(candidate: dict[str, object]) -> list[str]:
    english = str(candidate["normalizedEnglish"])
    translation = str(candidate["vietnamese"])
    example = str(candidate["example"])
    reasons: list[str] = []
    if not re.fullmatch(r"[a-z][a-z .'-]{0,79}", english) or len(english.split()) > 5:
        reasons.append("INVALID_LEARNER_HEADWORD")
    if not 2 <= len(translation) <= 180 or not re.search(r"[A-Za-zÀ-ỹ]", translation):
        reasons.append("INVALID_VIETNAMESE_LENGTH_OR_TEXT")
    if normalize(translation) == english:
        reasons.append("TRANSLATION_EQUALS_HEADWORD")
    if MARKUP.search(translation):
        reasons.append("UNSAFE_TRANSLATION_MARKUP")
    if translation.startswith((",", ";", ":")):
        reasons.append("MALFORMED_DICTIONARY_DEFINITION")
    if USAGE_LABEL.search(translation):
        reasons.append("NON_CORE_USAGE_LABEL")
    if BLOCKED_CONTENT.search(" ".join((english, translation, example))):
        reasons.append("INAPPROPRIATE_CONTENT")
    tokens = re.findall(r"[A-Za-zÀ-ỹ']+", example)
    if not 10 <= len(example) <= 220 or not 3 <= len(tokens) <= 30:
        reasons.append("INVALID_EXAMPLE_LENGTH")
    if english not in normalize(example):
        reasons.append("EXAMPLE_MISSING_HEADWORD")
    if GENERIC_EXAMPLE.search(example):
        reasons.append("GENERIC_OR_PLACEHOLDER_EXAMPLE")
    if not candidate["pronunciation"]:
        reasons.append("MISSING_VALID_IPA")
    if not candidate["topicCodes"]:
        reasons.append("TOPIC_UNCLASSIFIED")
    elif candidate.get("topicClassificationMethod", "MEANING_AND_SOURCE_CONFIRMED") != "MEANING_AND_SOURCE_CONFIRMED":
        reasons.append("TOPIC_NOT_SEMANTICALLY_CONFIRMED")
    if candidate["cefrStatus"] != "exact":
        reasons.append(str(candidate["cefrStatusReason"]))
    return sorted(set(reasons))


def candidate_score(candidate: dict[str, object]) -> int:
    score = 0
    translation = str(candidate["vietnamese"])
    example = str(candidate["example"])
    raw_pos = str(candidate["source"]["dictionaryPos"])  # type: ignore[index]
    if raw_pos in {"noun", "verb", "adj", "adv", "pron"}:
        score += 8
    if len(translation) <= 90:
        score += 5
    if 5 <= len(re.findall(r"[A-Za-z']+", example)) <= 18:
        score += 5
    if "(" not in translation and ")" not in translation:
        score += 3
    score += min(6, len(candidate["sourceTopics"]) * 2)  # type: ignore[arg-type]
    return score


def round_robin_select(candidates: list[dict[str, object]], target: int,
                       existing_count: int) -> list[dict[str, object]]:
    needed = max(0, target - existing_count)
    level_ratio = {"A1": .28, "A2": .25, "B1": .27, "B2": .20}
    quotas = {level: int(needed * ratio) for level, ratio in level_ratio.items()}
    quotas["A1"] += needed - sum(quotas.values())
    selected: list[dict[str, object]] = []
    selected_ids: set[str] = set()
    sense_counts: collections.Counter[tuple[str, str]] = collections.Counter()
    topic_meanings: set[tuple[str, str]] = set()

    def accept(item: dict[str, object]) -> bool:
        key = (str(item["normalizedEnglish"]), str(item["partOfSpeech"]))
        meaning_key = (str(item["primaryTopicCode"]), normalize(str(item["vietnamese"])))
        if sense_counts[key] >= 6 or meaning_key in topic_meanings:
            return False
        selected.append(item); selected_ids.add(str(item["candidateId"])); sense_counts[key] += 1
        topic_meanings.add(meaning_key)
        return True

    for level in ("A1", "A2", "B1", "B2"):
        buckets: dict[str, list[dict[str, object]]] = collections.defaultdict(list)
        for item in candidates:
            if item["cefr"] == level:
                buckets[str(item["primaryTopicCode"])].append(item)
        for values in buckets.values():
            values.sort(key=lambda item: (-int(item["qualityScore"]), str(item["candidateId"])))
        codes = sorted(buckets)
        while len([item for item in selected if item["cefr"] == level]) < quotas[level] and codes:
            progressed = False
            for code in list(codes):
                while buckets[code]:
                    item = buckets[code].pop(0)
                    if accept(item): progressed = True; break
                if not buckets[code]: codes.remove(code)
                if len([item for item in selected if item["cefr"] == level]) >= quotas[level]: break
            if not progressed: break
    for item in sorted(candidates, key=lambda value: (-int(value["qualityScore"]), str(value["candidateId"]))):
        if len(selected) >= needed: break
        if str(item["candidateId"]) not in selected_ids:
            accept(item)
    return selected[:needed]


def write_jsonl(path: Path, rows: Iterable[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        for row in rows:
            stream.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")


def build(target_total: int = 5000) -> dict[str, object]:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    with CEFR_PATH.open(encoding="utf-8-sig", newline="") as stream:
        cefr_rows = list(csv.DictReader(stream, delimiter=";"))
    cefr_groups: dict[tuple[str, str], list[dict[str, str]]] = collections.defaultdict(list)
    for row in cefr_rows:
        cefr_groups[(normalize(row["headword"]), normalize(row["pos"]))].append(row)
    by_word_pos: dict[tuple[str, str], dict[str, object]] = {}
    for (word, pos), rows in cefr_groups.items():
        levels = sorted({clean(row["CEFR"]).upper() for row in rows})
        labels, topics = source_topics(rows)
        by_word_pos[(word, pos)] = {"rows": rows, "levels": levels, "labels": labels,
                                            "topics": topics}

    with EXISTING_PATH.open(encoding="utf-8-sig", newline="") as stream:
        existing = list(csv.DictReader(stream))
    existing_pairs = {(normalize(row["english"]), normalize(row["vietnamese"])) for row in existing}

    connection = sqlite3.connect(f"file:{DICTIONARY_PATH.as_posix()}?mode=ro", uri=True)
    connection.row_factory = sqlite3.Row
    if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
        raise RuntimeError("SQLite integrity_check failed")
    relevant_words = {word for word, pos in by_word_pos if pos in SUPPORTED_POS}
    pronunciations: dict[str, list[dict[str, object]]] = collections.defaultdict(list)
    for row in connection.execute("""SELECT w.word,p.id,p.ipa,p.region FROM words w
        JOIN pronunciations p ON p.word_id=w.id"""):
        word = normalize(row["word"])
        if word in relevant_words:
            pronunciations[word].append(dict(row))

    candidates: list[dict[str, object]] = []
    seen_source_records: set[tuple[int, str]] = set()
    query = """SELECT w.id word_id,w.word,wd.id link_id,d.id definition_id,d.definition,d.pos,d.sub_pos,wd.example
        FROM words w JOIN word_definitions wd ON wd.word_id=w.id
        JOIN definitions d ON d.id=wd.definition_id ORDER BY w.id,wd.id"""
    for row in connection.execute(query):
        word = normalize(row["word"])
        canonical_pos = POS_MAP.get(row["pos"])
        if word not in relevant_words or canonical_pos is None:
            continue
        group = by_word_pos.get((word, canonical_pos))
        if not group:
            continue
        record_key = (int(row["link_id"]), canonical_pos)
        if record_key in seen_source_records:
            continue
        seen_source_records.add(record_key)
        levels = list(group["levels"])
        topics = list(group["topics"])
        labels = list(group["labels"])
        pronunciation, variants = choose_ipa(pronunciations[word])
        cefr_status = "exact" if len(levels) == 1 else "uncertain"
        classified_topics, topic_method = classify_topic(clean(row["word"]), clean(row["definition"]), topics)
        candidate: dict[str, object] = {
            "schemaVersion": 1,
            "candidateId": stable_id(row["word_id"], row["definition_id"], row["link_id"], canonical_pos),
            "normalizedKey": "\u0000".join((word, canonical_pos, normalize(row["definition"]))),
            "headword": clean(row["word"]), "normalizedEnglish": word,
            "partOfSpeech": canonical_pos, "cefr": levels[0] if len(levels) == 1 else None,
            "cefrStatus": cefr_status,
            "cefrStatusReason": "EXACT_HEADWORD_POS" if cefr_status == "exact" else "SOURCE_CEFR_CONFLICT",
            "cefrScope": "HEADWORD_POS_NOT_SENSE", "vietnamese": clean(row["definition"]),
            "pronunciation": pronunciation, "pronunciationVariants": variants,
            "example": clean(row["example"]), "exampleVi": None,
            "sourceTopics": labels, "topicCodes": classified_topics,
            "primaryTopicCode": classified_topics[0] if classified_topics else None,
            "topicClassificationMethod": topic_method,
            "senseEvidence": "SQLITE_WORD_DEFINITION_LINK",
            "source": {"name": "Skypedia English-Vietnamese Dictionary", "version": SOURCE_COMMIT,
                "url": SOURCE_URL, "license": SOURCE_LICENSE, "wordId": int(row["word_id"]),
                "definitionId": int(row["definition_id"]), "wordDefinitionId": int(row["link_id"]),
                "dictionaryPos": row["pos"], "subPos": row["sub_pos"]},
            "validationStatus": "NEEDS_REVIEW", "validationReasons": [],
            "importEligible": False, "quizEligible": False,
        }
        reasons = quality_reasons(candidate)
        if (word, normalize(candidate["vietnamese"])) in existing_pairs:
            reasons.append("EXISTING_FLASHCARD_PRESERVED")
        candidate["validationReasons"] = sorted(set(reasons))
        candidate["qualityScore"] = candidate_score(candidate)
        candidates.append(candidate)
    connection.close()

    duplicate_groups: dict[str, list[dict[str, object]]] = collections.defaultdict(list)
    for candidate in candidates:
        duplicate_groups[str(candidate["normalizedKey"])].append(candidate)
    for group in duplicate_groups.values():
        if len(group) > 1:
            group.sort(key=lambda item: str(item["candidateId"]))
            for duplicate in group[1:]:
                duplicate["validationReasons"] = sorted(set(duplicate["validationReasons"] +
                                                              ["DUPLICATE_NORMALIZED_SENSE"]))

    sense_groups = collections.Counter((str(item["normalizedEnglish"]), str(item["partOfSpeech"]))
                                       for item in candidates)
    for candidate in candidates:
        key = (str(candidate["normalizedEnglish"]), str(candidate["partOfSpeech"]))
        candidate["multiSenseCount"] = sense_groups[key]
        candidate["selectionEvidence"] = ["SQLITE_WORD_DEFINITION_LINK", "EXACT_CEFR_HEADWORD_POS"]
        if sense_groups[key] > 1:
            candidate["selectionEvidence"].append("RANKED_MULTI_SENSE_SELECTION")

    possible_duplicate_ids = near_duplicate_losers(candidates)
    for candidate in candidates:
        if str(candidate["candidateId"]) in possible_duplicate_ids:
            candidate["validationReasons"] = sorted(set(candidate["validationReasons"] +
                                                          ["POSSIBLE_DUPLICATE_MEANING"]))

    eligible: list[dict[str, object]] = []
    for candidate in candidates:
        reasons = set(candidate["validationReasons"])
        if "POSSIBLE_DUPLICATE_MEANING" in reasons:
            candidate["validationStatus"] = "NEEDS_REVIEW"
        elif reasons & HARD_REJECT_REASONS:
            candidate["validationStatus"] = "REJECTED"
        else:
            eligible.append(candidate)

    selected = round_robin_select(eligible, target_total, len(existing))
    selected_ids = {str(candidate["candidateId"]) for candidate in selected}
    key_counts = collections.Counter((str(item["normalizedEnglish"]), str(item["partOfSpeech"]))
                                     for item in selected)
    topic_translation_counts = collections.Counter((str(item["primaryTopicCode"]), normalize(str(item["vietnamese"])))
                                                   for item in selected)
    for candidate in eligible:
        if str(candidate["candidateId"]) in selected_ids:
            candidate["validationStatus"] = "APPROVED_AUTO"
            candidate["importEligible"] = True
            key = (str(candidate["normalizedEnglish"]), str(candidate["partOfSpeech"]))
            meaning_key = (str(candidate["primaryTopicCode"]), normalize(str(candidate["vietnamese"])))
            candidate["quizEligible"] = key_counts[key] == 1 and topic_translation_counts[meaning_key] == 1
        else:
            candidate["validationStatus"] = "NEEDS_REVIEW"
            candidate["validationReasons"] = ["DEFERRED_AFTER_5000_TARGET"]

    candidates.sort(key=lambda item: (str(item["validationStatus"]), str(item["candidateId"])))
    approved = [item for item in candidates if item["validationStatus"] == "APPROVED_AUTO"]
    review = [item for item in candidates if item["validationStatus"] == "NEEDS_REVIEW"]
    rejected = [item for item in candidates if item["validationStatus"] == "REJECTED"]
    status_counts = collections.Counter(str(item["validationStatus"]) for item in candidates)
    level_counts = collections.Counter(str(item["cefr"]) for item in approved)
    topic_counts = collections.Counter(str(item["primaryTopicCode"]) for item in approved)
    reason_counts = collections.Counter(reason for item in candidates for reason in item["validationReasons"])
    stats: dict[str, object] = {
        "generatedAtPolicy": "deterministic-no-wall-clock-field",
        "targetTotalFlashcards": target_total, "existingFlashcards": len(existing),
        "newApproved": len(approved), "totalAfterStagingImport": len(existing) + len(approved),
        "source": {"cefrRows": len(cefr_rows), "cefrHeadwords": len({normalize(r["headword"]) for r in cefr_rows}),
            "cefrHeadwordPosKeys": len(cefr_groups), "dictionaryBytes": DICTIONARY_PATH.stat().st_size,
            "dictionarySha256": sha256_file(DICTIONARY_PATH), "dictionaryCommit": SOURCE_COMMIT,
            "dictionaryLicense": SOURCE_LICENSE},
        "candidates": len(candidates), "status": dict(sorted(status_counts.items())),
        "approvedLevels": dict(sorted(level_counts.items())),
        "approvedPrimaryTopics": dict(sorted(topic_counts.items())),
        "approvedQuizEligible": sum(bool(item["quizEligible"]) for item in approved),
        "validationReasons": dict(sorted(reason_counts.items())),
    }
    write_jsonl(OUTPUT_DIR / "bivoca_vocab_candidates.jsonl", candidates)
    write_jsonl(OUTPUT_DIR / "bivoca_vocab_approved.jsonl", approved)
    write_jsonl(OUTPUT_DIR / "bivoca_vocab_review_queue.jsonl", review)
    write_jsonl(OUTPUT_DIR / "bivoca_vocab_rejected.jsonl", rejected)
    (OUTPUT_DIR / "bivoca_topics.json").write_text(json.dumps([
        {"code": code, "name": values[0], "description": values[1], "existing": values[2]}
        for code, values in TOPICS.items()], ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUTPUT_DIR / "bivoca_vocab_statistics.json").write_text(
        json.dumps(stats, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report = f"""# BiVoca vocabulary validation report

- Target total: {target_total:,}
- Existing protected Flashcards: {len(existing):,}
- New APPROVED_AUTO: {len(approved):,}
- NEEDS_REVIEW: {len(review):,}
- REJECTED: {len(rejected):,}
- Total eligible after staging import: {len(existing) + len(approved):,}
- Quiz-eligible new senses: {stats['approvedQuizEligible']:,}

APPROVED_AUTO requires exact CEFR headword/POS, a verified SQLite word-definition relation,
valid Vietnamese text, source IPA, a bounded example containing the headword, mapped source-topic
metadata, safe content, and a unique normalized sense. At most six ranked senses per headword/POS are
selected. CEFR remains explicitly scoped to headword/POS rather than asserted per sense.

The English examples are source-provided/generated. Passing automated checks does not constitute
human semantic certification. Deferred and ambiguous records remain in the review queue.

## Approved CEFR distribution

{json.dumps(dict(sorted(level_counts.items())), ensure_ascii=False, indent=2)}

## Approved primary-topic distribution

{json.dumps(dict(sorted(topic_counts.items())), ensure_ascii=False, indent=2)}

## Validation reasons

{json.dumps(dict(sorted(reason_counts.items())), ensure_ascii=False, indent=2)}
"""
    (OUTPUT_DIR / "bivoca_vocab_validation_report.md").write_text(report, encoding="utf-8", newline="\n")
    return stats


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target-total", type=int, default=5000)
    args = parser.parse_args()
    if not 300 <= args.target_total <= 100000:
        parser.error("--target-total must be between 300 and 100000")
    print(json.dumps(build(args.target_total), ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
