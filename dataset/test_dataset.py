"""Run stdlib tests; --integration additionally uses a disposable DB schema.

DB_* must point to a test database. The integration suite creates/drops only
its own UUID schema and never modifies tables in public or another schema.
"""
from __future__ import annotations

import argparse
import csv
import json
import os
import subprocess
import tempfile
import unittest
import uuid
from pathlib import Path

from import_flashcards import apply_sql, build_sql, find_psql
from validate_dataset import ROOT, CARD_FIELDS, TOPIC_FIELDS, validate


def write_csv(path, fields, rows):
    with path.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(rows)


class ValidationTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory(prefix='flashcard-validation-')
        self.path = Path(self.folder.name) / 'cards.csv'
        self.seed = validate(ROOT / 'flashcards.csv', ROOT / 'topics.csv', seed=True)
        self.card = dict(self.seed.cards[0])

    def tearDown(self):
        self.folder.cleanup()

    def check_rows(self, rows, topics=ROOT / 'topics.csv'):
        write_csv(self.path, CARD_FIELDS, rows)
        return validate(self.path, topics)

    def test_seed(self):
        self.assertTrue(self.seed.ok, self.seed.issues)
        self.assertEqual(len(self.seed.cards), 300)
        self.assertEqual(len({r['english'].lower() for r in self.seed.cards}), 300)
        self.assertEqual((ROOT / 'seed_flashcards.sql').read_text(encoding='utf-8'),
                         build_sql(self.seed.cards), 'Regenerate SQL after changing CSV')

    def test_extension_301_and_seed_rejection(self):
        addition = validate(ROOT / 'additions' / 'example_addition.csv').cards[0]
        result = self.check_rows(self.seed.cards + [addition])
        self.assertTrue(result.ok)
        self.assertFalse(validate(self.path, ROOT / 'topics.csv', seed=True).ok)

    def test_normalized_duplicate(self):
        another = dict(self.card, english=' APPLE ', vietnamese=' quả táo ')
        self.assertFalse(self.check_rows([self.card, another]).ok)

    def test_distinct_senses(self):
        rows = validate(ROOT / 'additions' / 'example_addition.csv').cards[1:]
        self.assertTrue(self.check_rows(rows).ok)

    def test_missing_and_invalid_enums(self):
        for changes in ({'example_vi': ''}, {'level': 'C1'}, {'part_of_speech': 'Noun'},
                        {'topic_code': 'UNKNOWN'}, {'pronunciation': 'apple'}, {'english': 'word1'}):
            with self.subTest(changes=changes):
                self.assertFalse(self.check_rows([dict(self.card, **changes)]).ok)

    def test_bad_header_and_utf8(self):
        self.path.write_bytes(b'topic_id,english\n1,apple\n')
        self.assertFalse(validate(self.path, ROOT / 'topics.csv').ok)
        self.path.write_bytes(b'\xff\xfe')
        self.assertFalse(validate(self.path, ROOT / 'topics.csv').ok)

    def test_malformed_csv(self):
        self.path.write_text(','.join(CARD_FIELDS) + '\n"unfinished', encoding='utf-8')
        self.assertFalse(validate(self.path, ROOT / 'topics.csv').ok)

    def test_empty_dataset(self):
        self.assertFalse(self.check_rows([]).ok)

    def test_inflection_warning_does_not_fail(self):
        result = self.check_rows([dict(self.card, example='Fresh apples are on the table.')])
        self.assertTrue(result.ok)
        self.assertTrue(any(i.category == 'example' and i.warning for i in result.issues))

    def test_new_topic_without_changing_old_topics(self):
        path = Path(self.folder.name) / 'topics.csv'
        write_csv(path, TOPIC_FIELDS, self.seed.topics + [
            {'code': 'SPORT', 'name': 'Sport', 'description': 'Thể thao', 'image_url': ''}])
        new = dict(self.card, topic_code='SPORT', english='football', vietnamese='bóng đá',
                   pronunciation='/ˈfʊtbɔːl/', example='We play football after school every day.',
                   example_vi='Chúng tôi chơi bóng đá sau giờ học mỗi ngày.')
        self.assertTrue(self.check_rows([new], path).ok)


class PostgreSQLTests(unittest.TestCase):
    @classmethod
    def raw(cls, sql):
        env = os.environ.copy()
        for key, target in [('DB_HOST', 'PGHOST'), ('DB_PORT', 'PGPORT'),
                            ('DB_NAME', 'PGDATABASE'), ('DB_USER', 'PGUSER'),
                            ('DB_PASSWORD', 'PGPASSWORD')]:
            env[target] = env[key]
        env['PGCLIENTENCODING'] = 'UTF8'
        process = subprocess.run([find_psql(), '-X', '-q', '-t', '-A', '-v',
                                  'ON_ERROR_STOP=1', '-f', '-'], input=sql,
                                 text=True, encoding='utf-8', capture_output=True, env=env)
        if process.returncode:
            raise RuntimeError(process.stderr)
        return process.stdout.strip()

    @classmethod
    def setUpClass(cls):
        cls.schema = 'fc_test_' + uuid.uuid4().hex
        cls.old_options = os.environ.get('PGOPTIONS')
        cls.raw(f'CREATE SCHEMA "{cls.schema}";')
        # Only the new schema is in search_path; public is deliberately excluded.
        os.environ['PGOPTIONS'] = f'-c search_path={cls.schema}'
        try:
            cls.raw((ROOT / 'schema.sql').read_text(encoding='utf-8'))
            # Non-default starting ID proves the code does not assume Travel = 3.
            cls.raw('ALTER SEQUENCE topics_id_seq RESTART WITH 101;')
            cls.raw((ROOT / 'seed_topics.sql').read_text(encoding='utf-8'))
            cls.seed_sql = (ROOT / 'seed_flashcards.sql').read_text(encoding='utf-8')
            cls.first_seed = json.loads(cls.raw(cls.seed_sql))
        except BaseException:
            cls.tearDownClass()
            raise

    @classmethod
    def tearDownClass(cls):
        try:
            # Only the exact schema created above is ever dropped.
            cls.raw(f'DROP SCHEMA "{cls.schema}" CASCADE;')
        finally:
            if cls.old_options is None:
                os.environ.pop('PGOPTIONS', None)
            else:
                os.environ['PGOPTIONS'] = cls.old_options

    def test_full_incremental_lifecycle(self):
        self.assertEqual({k: self.first_seed[k] for k in ('inserted', 'failed')},
                         {'inserted': 300, 'failed': 0})
        self.assertEqual(self.raw('SELECT COUNT(*) FROM topics;'), '10')
        self.assertEqual(self.raw('SELECT MIN(id) FROM topics;'), '101')
        self.assertEqual(self.raw('SELECT COUNT(*) FROM flashcards;'), '300')
        self.assertEqual(self.raw('SELECT COUNT(*) FROM (SELECT topic_id FROM flashcards '
                                 'GROUP BY topic_id HAVING COUNT(*) <> 30) bad;'), '0')
        repeat = apply_sql(self.seed_sql)
        self.assertEqual(repeat['skipped'], 300)
        self.assertEqual(repeat['inserted'], 0)
        self.raw((ROOT / 'seed_topics.sql').read_text(encoding='utf-8'))
        self.assertEqual(self.raw('SELECT COUNT(*) FROM topics;'), '10')

        addition_path = ROOT / 'additions' / 'example_addition.csv'
        additions = validate(addition_path, ROOT / 'topics.csv').cards
        original = self.raw("SELECT id || ':' || created_at::text FROM flashcards WHERE english='book';")
        summary = apply_sql(build_sql(additions))
        self.assertEqual([summary[k] for k in ('inserted', 'updated', 'skipped', 'failed')], [2, 1, 0, 0])
        self.assertEqual(self.raw('SELECT COUNT(*) FROM flashcards;'), '302')
        self.assertEqual(self.raw("SELECT COUNT(*) FROM flashcards WHERE english='book';"), '2')
        self.assertEqual(self.raw("SELECT id || ':' || created_at::text FROM flashcards "
                                 "WHERE english='book' AND vietnamese='quyển sách';"), original)
        summary = apply_sql(build_sql(additions))
        self.assertEqual(summary['skipped'], 3)

        # Changing topic updates the existing record and keeps its inactive state.
        self.raw("UPDATE flashcards SET is_active=FALSE WHERE english='watermelon';")
        moved = dict(additions[0], topic_code='SHOPPING')
        summary = apply_sql(build_sql([moved]))
        self.assertEqual(summary['updated'], 1)
        self.assertEqual(self.raw("SELECT t.code || ':' || f.is_active::text FROM flashcards f "
                                 "JOIN topics t ON t.id=f.topic_id WHERE english='watermelon';"), 'SHOPPING:false')

        case_variant = dict(additions[1], english='BOOK', vietnamese='QUYỂN SÁCH')
        summary = apply_sql(build_sql([case_variant]))
        self.assertEqual(summary['skipped'], 1, 'DB locale must lowercase Vietnamese correctly')

        unknown = dict(additions[0], topic_code='UNKNOWN')
        summary = apply_sql(build_sql([unknown]))
        self.assertEqual(summary['failed'], 1)
        self.assertEqual(summary['errors'][0]['row'], unknown['_row'])

        # SQL-escaping check: apostrophes, commas and backslashes remain literal.
        literal = "It's a book, not SQL. '; DROP TABLE topics; -- \\"
        escaped = dict(additions[1], example=literal)
        self.assertEqual(apply_sql(build_sql([escaped]))['updated'], 1)
        self.assertEqual(self.raw("SELECT example FROM flashcards WHERE english='book' "
                                 "AND vietnamese='quyển sách';"), literal)
        self.assertEqual(self.raw('SELECT COUNT(*) FROM topics;'), '10')
        self.assertEqual(self.raw('SELECT COUNT(*) FROM (SELECT LOWER(english), LOWER(vietnamese) '
                                 'FROM flashcards GROUP BY LOWER(english), LOWER(vietnamese) '
                                 'HAVING COUNT(*) > 1) duplicates;'), '0')

        # Production database topics can be added independently of the seed CSV.
        self.raw("INSERT INTO topics(code, name) VALUES ('SPORT', 'Sport');")
        sport = dict(additions[0], topic_code='SPORT', english='football', vietnamese='bóng đá',
                     pronunciation='/ˈfʊtbɔːl/', example='We play football in the park.',
                     example_vi='Chúng tôi chơi bóng đá trong công viên.')
        self.assertEqual(apply_sql(build_sql([sport]))['inserted'], 1)
        partial = apply_sql(build_sql([unknown, sport]))
        self.assertEqual([partial[k] for k in ('inserted', 'updated', 'skipped', 'failed')],
                         [0, 0, 1, 1])

        # The actual CLI rejects duplicate CSV before touching the database.
        with tempfile.TemporaryDirectory(prefix='fc-import-cli-') as folder:
            path = Path(folder) / 'duplicate.csv'
            write_csv(path, CARD_FIELDS, [additions[0], additions[0]])
            process = subprocess.run([os.sys.executable, str(ROOT / 'import_flashcards.py'),
                                      str(path), '--apply'], text=True, encoding='utf-8', capture_output=True)
            self.assertEqual(process.returncode, 1)
            self.assertIn('VALIDATION_FAILED', process.stdout)
            write_csv(path, CARD_FIELDS, [sport])
            process = subprocess.run([os.sys.executable, str(ROOT / 'import_flashcards.py'),
                                      str(path), '--apply'], text=True, encoding='utf-8', capture_output=True)
            self.assertEqual(process.returncode, 0, process.stderr)
            self.assertEqual(json.loads(process.stdout)['skipped'], 1)
            # Exercise the generated pgAdmin output, not just the in-memory SQL.
            sql_path = Path(folder) / 'addition.sql'
            process = subprocess.run([os.sys.executable, str(ROOT / 'import_flashcards.py'),
                                      str(path), '--sql-output', str(sql_path)],
                                     text=True, encoding='utf-8', capture_output=True)
            self.assertEqual(process.returncode, 0, process.stderr)
            self.assertEqual(json.loads(self.raw(sql_path.read_text(encoding='utf-8')))['skipped'], 1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--integration', action='store_true')
    args = parser.parse_args()
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(ValidationTests)
    if args.integration:
        suite.addTests(unittest.defaultTestLoader.loadTestsFromTestCase(PostgreSQLTests))
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    raise SystemExit(0 if result.wasSuccessful() else 1)
