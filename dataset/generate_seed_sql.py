"""Regenerate seed SQL from CSV without duplicating the vocabulary source."""
import sys

from import_flashcards import build_sql, sql_string
from validate_dataset import ROOT, validate, print_report


def main():
    result = validate(ROOT / 'flashcards.csv', ROOT / 'topics.csv')
    if not result.ok:
        print_report(result)
        return 1
    values = ',\n'.join('    (' + ', '.join(
        sql_string(row[key]) if row[key] else 'NULL'
        for key in ('code', 'name', 'description', 'image_url')) + ')'
        for row in result.topics)
    topics_sql = f"""-- Generated from topics.csv; internal IDs are never supplied.
BEGIN;
SET LOCAL standard_conforming_strings = on;
INSERT INTO topics (code, name, description, image_url)
VALUES
{values}
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    image_url = EXCLUDED.image_url,
    updated_at = CURRENT_TIMESTAMP;
COMMIT;
"""
    (ROOT / 'seed_topics.sql').write_text(topics_sql, encoding='utf-8', newline='\n')
    (ROOT / 'seed_flashcards.sql').write_text(build_sql(result.cards), encoding='utf-8', newline='\n')
    print(f'Generated seed_topics.sql ({len(result.topics)} topics) and seed_flashcards.sql ({len(result.cards)} cards).')
    return 0


if __name__ == '__main__':
    sys.exit(main())
