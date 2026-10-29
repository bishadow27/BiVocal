"""Incremental PostgreSQL import via psql, or generate SQL for pgAdmin."""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

from validate_dataset import ROOT, CARD_FIELDS, validate


def sql_string(value: str) -> str:
    # SQL runs with standard_conforming_strings=on; apostrophes are doubled.
    return "'" + value.replace("'", "''") + "'"


def build_sql(rows: list[dict]) -> str:
    values = ',\n'.join('    (' + ', '.join(
        [str(row['_row'])] + [sql_string(row[key]) for key in CARD_FIELDS]) + ')'
        for row in rows)
    return f"""-- Generated from validated UTF-8 CSV; topic IDs are resolved at runtime.
-- Valid rows commit even if another row fails. Read the summary and failure list.
BEGIN;
SET LOCAL standard_conforming_strings = on;
CREATE TEMP TABLE _fc_import_input (
    source_row INTEGER, topic_code TEXT, english TEXT, vietnamese TEXT,
    pronunciation TEXT, part_of_speech TEXT, example TEXT, example_vi TEXT, level TEXT
) ON COMMIT DROP;
INSERT INTO _fc_import_input VALUES
{values};
CREATE TEMP TABLE _fc_import_result (
    source_row INTEGER, english TEXT, outcome TEXT, error TEXT
) ON COMMIT DROP;
DO $fc_import$
DECLARE
    r RECORD;
    v_topic INTEGER;
    v_existing INTEGER;
    v_changed BOOLEAN;
BEGIN
    -- Serialize writes during classification, so inserted/updated/skipped are exact.
    LOCK TABLE topics IN SHARE MODE;
    LOCK TABLE flashcards IN SHARE ROW EXCLUSIVE MODE;
    FOR r IN SELECT * FROM _fc_import_input ORDER BY source_row LOOP
        BEGIN
            SELECT id INTO v_topic FROM topics WHERE code = r.topic_code;
            IF NOT FOUND THEN
                RAISE EXCEPTION 'Unknown topic_code: %', r.topic_code;
            END IF;
            SELECT id,
                ROW(topic_id, pronunciation, part_of_speech, example, example_vi, level)
                IS DISTINCT FROM
                ROW(v_topic, r.pronunciation, r.part_of_speech, r.example, r.example_vi, r.level)
            INTO v_existing, v_changed
            FROM flashcards
            WHERE LOWER(english) = LOWER(r.english COLLATE dataset_unicode)
              AND LOWER(vietnamese) = LOWER(r.vietnamese COLLATE dataset_unicode);
            IF v_existing IS NOT NULL AND NOT v_changed THEN
                INSERT INTO _fc_import_result VALUES (r.source_row, r.english, 'skipped', NULL);
                CONTINUE;
            END IF;
            INSERT INTO flashcards AS f (
                topic_id, english, vietnamese, pronunciation, part_of_speech,
                example, example_vi, level
            ) VALUES (
                v_topic, r.english, r.vietnamese, r.pronunciation, r.part_of_speech,
                r.example, r.example_vi, r.level
            )
            ON CONFLICT (LOWER(english), LOWER(vietnamese)) DO UPDATE SET
                topic_id = EXCLUDED.topic_id,
                pronunciation = EXCLUDED.pronunciation,
                part_of_speech = EXCLUDED.part_of_speech,
                example = EXCLUDED.example,
                example_vi = EXCLUDED.example_vi,
                level = EXCLUDED.level,
                updated_at = CURRENT_TIMESTAMP
            WHERE ROW(f.topic_id, f.pronunciation, f.part_of_speech, f.example, f.example_vi, f.level)
                IS DISTINCT FROM
                ROW(EXCLUDED.topic_id, EXCLUDED.pronunciation, EXCLUDED.part_of_speech,
                    EXCLUDED.example, EXCLUDED.example_vi, EXCLUDED.level);
            INSERT INTO _fc_import_result VALUES (
                r.source_row, r.english,
                CASE WHEN v_existing IS NULL THEN 'inserted' ELSE 'updated' END, NULL
            );
        EXCEPTION WHEN OTHERS THEN
            -- PostgreSQL rolls back this row's changes inside the exception block.
            INSERT INTO _fc_import_result VALUES (r.source_row, r.english, 'failed', SQLERRM);
        END;
    END LOOP;
END
$fc_import$;
SELECT json_build_object(
    'inserted', COUNT(*) FILTER (WHERE outcome = 'inserted'),
    'updated', COUNT(*) FILTER (WHERE outcome = 'updated'),
    'skipped', COUNT(*) FILTER (WHERE outcome = 'skipped'),
    'failed', COUNT(*) FILTER (WHERE outcome = 'failed'),
    'errors', COALESCE(json_agg(json_build_object(
        'row', source_row, 'english', english, 'error', error
    ) ORDER BY source_row) FILTER (WHERE outcome = 'failed'), '[]'::json)
) FROM _fc_import_result;
COMMIT;
"""


def find_psql(explicit: str | None = None) -> str:
    executable = explicit or os.environ.get('PSQL_PATH') or shutil.which('psql')
    if not executable and os.name == 'nt':
        base = Path(os.environ.get('ProgramFiles', r'C:\Program Files')) / 'PostgreSQL'
        versions = sorted(base.glob('*/bin/psql.exe'),
                          key=lambda p: tuple(int(n) for n in p.parents[1].name.split('.') if n.isdigit()),
                          reverse=True)
        executable = str(versions[0]) if versions else None
    if not executable:
        raise ValueError('psql not found. Set PSQL_PATH or --psql, or use --sql-output.')
    return executable


def apply_sql(sql: str, psql: str | None = None) -> dict:
    required = ['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD']
    missing = [key for key in required if not os.environ.get(key)]
    if missing:
        raise ValueError('Missing environment variables: ' + ', '.join(missing))
    env = os.environ.copy()
    for key in required:
        target = {'DB_HOST': 'PGHOST', 'DB_PORT': 'PGPORT', 'DB_NAME': 'PGDATABASE',
                  'DB_USER': 'PGUSER', 'DB_PASSWORD': 'PGPASSWORD'}[key]
        env[target] = env[key]
    env['PGCLIENTENCODING'] = 'UTF8'
    env.setdefault('PGCONNECT_TIMEOUT', '10')
    process = subprocess.run(
        [find_psql(psql), '-X', '--no-password', '--quiet', '--tuples-only',
         '--no-align', '--set=ON_ERROR_STOP=1', '--file=-'],
        input=sql, text=True, encoding='utf-8', capture_output=True, env=env,
    )
    if process.returncode:
        # A connection loss at COMMIT can leave an uncertain outcome.
        raise RuntimeError(process.stderr.strip() or 'psql failed')
    if process.stderr.strip():
        print(process.stderr.strip(), file=sys.stderr)
    try:
        return json.loads(process.stdout.strip())
    except ValueError as exc:
        raise RuntimeError('Database response unreadable; verify outcome before retrying') from exc


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('csv_file', type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--apply', action='store_true', help='Connect using DB_* environment variables')
    mode.add_argument('--sql-output', type=Path, help='Write SQL; no database connection')
    parser.add_argument('--topics', type=Path, help='Optional local topic check; DB is authoritative')
    parser.add_argument('--psql', help='Path to psql executable')
    args = parser.parse_args()
    result = validate(args.csv_file, args.topics)
    for issue in result.issues:
        label = 'WARNING' if issue.warning else 'ERROR'
        print(f'{label} file={issue.file} row={issue.row} english={issue.english!r} error={issue.error}',
              file=sys.stderr)
    if not result.ok:
        failed = len({i.row for i in result.issues if not i.warning and i.row > 1})
        print(json.dumps({'inserted': 0, 'updated': 0, 'skipped': 0, 'failed': failed,
                          'status': 'VALIDATION_FAILED; no SQL executed'}, ensure_ascii=False))
        return 1
    sql = build_sql(result.cards)
    try:
        if args.sql_output:
            if args.sql_output.resolve() == args.csv_file.resolve() or (
                args.topics and args.sql_output.resolve() == args.topics.resolve()
            ):
                raise ValueError('SQL output must not overwrite an input CSV')
            args.sql_output.parent.mkdir(parents=True, exist_ok=True)
            args.sql_output.write_text(sql, encoding='utf-8', newline='\n')
            print(f'SQL GENERATED: {args.sql_output.resolve()} ({len(result.cards)} rows)')
            print('Inserted/updated/skipped/failed: NOT VERIFIED until SQL runs in PostgreSQL.')
            return 0
        summary = apply_sql(sql, args.psql)
        errors = summary.pop('errors')
        print(json.dumps(summary, ensure_ascii=False))
        for error in errors:
            print(f"ERROR file={args.csv_file} row={error['row']} english={error['english']!r} error={error['error']}")
        return 1 if summary['failed'] else 0
    except (OSError, ValueError, RuntimeError) as exc:
        # Never print environment variables or connection strings.
        message = str(exc)
        secret = os.environ.get('DB_PASSWORD')
        if secret:
            message = message.replace(secret, '[REDACTED]')
        print(f'IMPORT FAILED: {message}', file=sys.stderr)
        uncertain = isinstance(exc, RuntimeError)
        print(json.dumps({'inserted': None if uncertain else 0,
                          'updated': None if uncertain else 0,
                          'skipped': None if uncertain else 0,
                          'failed': None if uncertain else len(result.cards),
                          'status': 'DATABASE_RESULT_NOT_VERIFIED' if uncertain else 'NO_SQL_EXECUTED'}))
        return 1


if __name__ == '__main__':
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, 'reconfigure'):
            stream.reconfigure(encoding='utf-8')
    sys.exit(main())
