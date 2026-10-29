"""Validate UTF-8 vocabulary CSV files; no third-party dependencies."""
from __future__ import annotations

import argparse
import csv
import re
import sys
import unicodedata
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent
TOPIC_FIELDS = ['code', 'name', 'description', 'image_url']
CARD_FIELDS = ['topic_code', 'english', 'vietnamese', 'pronunciation',
               'part_of_speech', 'example', 'example_vi', 'level']
LEVELS = ('A1', 'A2', 'B1', 'B2')
PARTS = {'noun', 'verb', 'adjective', 'adverb', 'preposition',
         'conjunction', 'pronoun', 'phrase'}
SEED_CODES = {'FOOD', 'ANIMALS', 'TRAVEL', 'SCHOOL', 'FAMILY',
              'TECHNOLOGY', 'WORK', 'HEALTH', 'SHOPPING', 'WEATHER'}
LIMITS = {'topic_code': 50, 'english': 150, 'vietnamese': 255,
          'pronunciation': 150, 'part_of_speech': 50, 'level': 10}
IPA = re.compile(r'/[a-zɑɒæəɛɜɪʊʌɔθðŋʃʒɡˈˌːˑ. ()-]+/')


def clean(value: str) -> str:
    return unicodedata.normalize('NFC', value.strip())


def word_key(row: dict) -> tuple[str, str]:
    return clean(row['english']).lower(), clean(row['vietnamese']).lower()


@dataclass
class Issue:
    file: str
    row: int
    english: str
    error: str
    category: str
    warning: bool = False


@dataclass
class Result:
    topics: list[dict] = field(default_factory=list)
    cards: list[dict] = field(default_factory=list)
    issues: list[Issue] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not any(not i.warning for i in self.issues)

    def add(self, path, row, english, error, category, warning=False):
        self.issues.append(Issue(str(path), row, english, error, category, warning))


def read_csv(path: Path, fields: list[str], result: Result) -> list[dict]:
    rows = []
    try:
        with path.open('r', encoding='utf-8-sig', newline='') as stream:
            reader = csv.reader(stream, strict=True)
            header = next(reader, None)
            if header != fields:
                result.add(path, 1, '', f'Expected exact header: {",".join(fields)}', 'csv')
                return rows
            while True:
                start = reader.line_num + 1
                values = next(reader, None)
                if values is None:
                    break
                if len(values) != len(fields):
                    result.add(path, start, '', f'Expected {len(fields)} fields; got {len(values)}', 'csv')
                    continue
                row = dict(zip(fields, map(clean, values)))
                row['_row'] = start
                rows.append(row)
                for key, value in zip(fields, values):
                    if any(ord(c) < 32 and c not in '\n\r\t' for c in value):
                        result.add(path, start, row.get('english', ''), f'Control character in {key}', 'csv')
                    if value != clean(value):
                        result.add(path, start, row.get('english', ''),
                                   f'{key}: whitespace/NFC normalized during import', 'normalization', True)
    except (OSError, UnicodeError, csv.Error) as exc:
        row_number = locals().get('reader')
        result.add(path, getattr(row_number, 'line_num', 0), '', str(exc), 'csv')
    return rows


def validate(cards_path: Path, topics_path: Path | None = None, seed=False) -> Result:
    result = Result()
    codes = set()
    names = set()
    if topics_path is not None:
        result.topics = read_csv(topics_path, TOPIC_FIELDS, result)
        if not result.topics:
            result.add(topics_path, 0, '', 'At least one topic is required', 'topics')
        for row in result.topics:
            code, name = row['code'], row['name']
            if not re.fullmatch(r'[A-Z][A-Z0-9_]{0,49}', code):
                result.add(topics_path, row['_row'], '', 'Invalid topic code', 'topics')
            if not name or len(name) > 100:
                result.add(topics_path, row['_row'], '', 'Missing or oversized topic name', 'topics')
            if code in codes or name in names:
                result.add(topics_path, row['_row'], '', 'Duplicate topic code/name', 'topics')
            codes.add(code)
            names.add(name)
    result.cards = read_csv(cards_path, CARD_FIELDS, result)
    if not result.cards:
        result.add(cards_path, 0, '', 'At least one flashcard is required', 'volume')
    seen = {}
    english_seen = set()
    for row in result.cards:
        number, english = row['_row'], row['english']
        for key in CARD_FIELDS:
            value = row[key]
            if not value or value.lower() in {'null', 'none', 'todo', 'placeholder'}:
                result.add(cards_path, number, english, f'Missing/placeholder field: {key}', 'missing')
            if key in LIMITS and len(value) > LIMITS[key]:
                result.add(cards_path, number, english, f'{key} exceeds {LIMITS[key]} characters', 'length')
        if not re.fullmatch(r'[A-Z][A-Z0-9_]{0,49}', row['topic_code']):
            result.add(cards_path, number, english, 'Invalid topic code format', 'topic_code')
        elif topics_path is not None and row['topic_code'] not in codes:
            result.add(cards_path, number, english, 'Topic code not found in topics CSV', 'topic_code')
        if row['level'] not in LEVELS:
            result.add(cards_path, number, english, 'Invalid CEFR level', 'cefr')
        if row['part_of_speech'] not in PARTS:
            result.add(cards_path, number, english, 'Invalid part of speech', 'pos')
        if not IPA.fullmatch(row['pronunciation']):
            result.add(cards_path, number, english, 'Invalid IPA format (not a dictionary check)', 'ipa')
        if re.fullmatch(r'(?:word\d+|test|sample|todo|placeholder)', english, re.I):
            result.add(cards_path, number, english, 'Placeholder vocabulary is not allowed', 'placeholder')
        pair = word_key(row)
        if pair in seen:
            result.add(cards_path, number, english, f'Duplicate word + meaning; first row {seen[pair]}', 'duplicate')
        seen[pair] = number
        if pair[0] in english_seen and pair not in {('', '')}:
            result.add(cards_path, number, english, 'Repeated English word: manually review distinct sense', 'polysemy', True)
        english_seen.add(pair[0])
        pattern = r'(?<!\w)' + re.escape(english.lower()).replace(r'\ ', r'\s+') + r'(?!\w)'
        if english and not re.search(pattern, row['example'].lower()):
            result.add(cards_path, number, english,
                       'Example does not contain exact headword; check inflection/context', 'example', True)
        count = len(re.findall(r"[A-Za-z]+(?:['’-][A-Za-z]+)*", row['example']))
        if not 5 <= count <= 15:
            result.add(cards_path, number, english, f'Example has {count} words; target 5-15', 'example_length', True)
        if pair[0] in {'dog', 'mother', 'apple', 'school', 'rain'} and row['level'] == 'B2':
            result.add(cards_path, number, english, 'Basic word assigned B2', 'cefr')
    if seed:
        if len(result.topics) != 10 or codes != SEED_CODES:
            result.add(topics_path, 0, '', 'Seed needs the 10 specified topic codes', 'seed')
        if len(result.cards) != 300:
            result.add(cards_path, 0, '', 'Seed needs exactly 300 flashcards', 'seed')
        counts = Counter(row['topic_code'] for row in result.cards)
        if set(counts) != SEED_CODES or any(counts[code] != 30 for code in SEED_CODES):
            result.add(cards_path, 0, '', 'Seed needs exactly 30 flashcards per topic', 'distribution')
    return result


def print_report(result: Result, seed=False):
    errors = Counter(i.category for i in result.issues if not i.warning)
    print('DATASET VALIDATION\n')
    print(f'Topics: {len(result.topics)} ' + ('PASS' if not errors['topics'] and not errors['seed'] else 'FAIL'))
    print(f'Flashcards: {len(result.cards)} ' + ('PASS' if not errors['volume'] and not errors['seed'] else 'FAIL'))
    if seed:
        print('Seed distribution: 30/topic ' + ('FAIL' if errors['distribution'] else 'PASS'))
    for label, category in [('Duplicate word + meaning', 'duplicate'), ('Missing fields', 'missing'),
                            ('Invalid topic codes', 'topic_code'), ('Invalid CEFR levels', 'cefr'),
                            ('Invalid part of speech', 'pos'), ('Invalid IPA format', 'ipa'),
                            ('CSV / UTF-8 errors', 'csv')]:
        print(f'{label}: {errors[category]} ' + ('FAIL' if errors[category] else 'PASS'))
    print()
    levels = Counter(row['level'] for row in result.cards)
    for level in LEVELS:
        count = levels[level]
        percent = count / len(result.cards) * 100 if result.cards else 0
        print(f'{level}: {count} ({percent:.1f}%)')
    print('\nPer topic:')
    for code, count in sorted(Counter(row['topic_code'] for row in result.cards).items()):
        print(f'  {code}: {count}')
    for issue in result.issues:
        prefix = 'WARNING' if issue.warning else 'ERROR'
        print(f'{prefix} file={issue.file} row={issue.row} english={issue.english!r} error={issue.error}')
    print(f'\nWarnings: {sum(i.warning for i in result.issues)}')
    print('DATASET STATUS: ' + ('PASS' if result.ok else 'FAIL'))
    print('Grammar, translation, dictionary IPA and CEFR require editorial review.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--seed', action='store_true')
    parser.add_argument('--topics', type=Path, default=ROOT / 'topics.csv')
    parser.add_argument('--flashcards', type=Path, default=ROOT / 'flashcards.csv')
    args = parser.parse_args()
    result = validate(args.flashcards, args.topics, args.seed)
    print_report(result, args.seed)
    return 0 if result.ok else 1


if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    sys.exit(main())
