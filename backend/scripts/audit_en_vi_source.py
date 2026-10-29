"""Read-only, reproducible audit of the pinned English-Vietnamese SQLite source."""
from __future__ import annotations

import hashlib
import json
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "datasets" / "en-vi" / "dictionary_en_vi.db"
OUTPUT = ROOT / "datasets" / "processed" / "en_vi_source_audit.json"


def scalar(connection: sqlite3.Connection, sql: str) -> int:
    return int(connection.execute(sql).fetchone()[0])


def main() -> None:
    checksum = hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    connection = sqlite3.connect(f"file:{SOURCE.as_posix()}?mode=ro", uri=True)
    try:
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        foreign_keys = len(connection.execute("PRAGMA foreign_key_check").fetchall())
        controls = 0
        replacement = 0
        for table, column in (("words", "word"), ("definitions", "definition"),
                              ("word_definitions", "example"), ("pronunciations", "ipa")):
            for (value,) in connection.execute(f"SELECT {column} FROM {table} WHERE {column} IS NOT NULL"):
                text = str(value)
                controls += sum(ord(char) < 32 and char not in "\t\r\n" for char in text)
                replacement += text.count("\ufffd")
        result = {
            "sourceBytes": SOURCE.stat().st_size,
            "sha256": checksum,
            "sqliteIntegrityCheck": integrity,
            "foreignKeyViolations": foreign_keys,
            "words": scalar(connection, "SELECT count(*) FROM words"),
            "definitions": scalar(connection, "SELECT count(*) FROM definitions"),
            "wordDefinitionLinks": scalar(connection, "SELECT count(*) FROM word_definitions"),
            "pronunciations": scalar(connection, "SELECT count(*) FROM pronunciations"),
            "wordsWithPronunciation": scalar(connection, "SELECT count(DISTINCT word_id) FROM pronunciations WHERE trim(ipa)<>''"),
            "linksWithExample": scalar(connection, "SELECT count(*) FROM word_definitions WHERE trim(coalesce(example,''))<>''"),
            "linksMissingExample": scalar(connection, "SELECT count(*) FROM word_definitions WHERE trim(coalesce(example,''))=''"),
            "wordsWithMultipleSenses": scalar(connection, """SELECT count(*) FROM (SELECT word_id FROM word_definitions
                GROUP BY word_id HAVING count(DISTINCT definition_id)>1)"""),
            "wordsWithMultiplePartsOfSpeech": scalar(connection, """SELECT count(*) FROM (SELECT wd.word_id FROM word_definitions wd
                JOIN definitions d ON d.id=wd.definition_id GROUP BY wd.word_id HAVING count(DISTINCT d.pos)>1)"""),
            "wordsWithBothUkAndUsPronunciation": scalar(connection, """SELECT count(*) FROM (SELECT word_id FROM pronunciations
                GROUP BY word_id HAVING max(CASE WHEN lower(coalesce(region,'')) IN ('bre','uk') THEN 1 ELSE 0 END)=1
                AND max(CASE WHEN lower(coalesce(region,'')) IN ('name','us','general-american') THEN 1 ELSE 0 END)=1)"""),
            "normalizedDuplicateHeadwordGroups": scalar(connection, """SELECT count(*) FROM (SELECT lower(trim(word)),lang_code
                FROM words GROUP BY lower(trim(word)),lang_code HAVING count(*)>1)"""),
            "missingVietnameseDefinitions": scalar(connection, "SELECT count(*) FROM definitions WHERE trim(coalesce(definition,''))=''"),
            "headwordsOver80Characters": scalar(connection, "SELECT count(*) FROM words WHERE length(trim(word))>80"),
            "headwordsOverFiveTokens": scalar(connection, "SELECT count(*) FROM words WHERE length(trim(word))-length(replace(trim(word),' ',''))+1>5"),
            "unicodeControlCharacters": controls,
            "unicodeReplacementCharacters": replacement,
        }
    finally:
        connection.close()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
