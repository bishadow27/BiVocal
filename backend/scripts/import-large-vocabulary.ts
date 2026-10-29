import 'dotenv/config';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import pg from 'pg';

interface Candidate {
  candidateId: string; headword: string; normalizedEnglish: string; vietnamese: string;
  partOfSpeech: string; cefr: string; cefrScope: string; pronunciation: string;
  example: string; exampleVi: string | null; topicCodes: string[]; primaryTopicCode: string;
  validationStatus: string; validationReasons: string[]; importEligible: boolean; quizEligible: boolean;
  source: { wordId: number; definitionId: number; wordDefinitionId: number };
}
interface TopicInput { code: string; name: string; description: string; existing: boolean }

const apply = process.argv.includes('--apply');
const allowed = new Set(['--apply', '--dry-run']);
if (process.argv.slice(2).some(argument => !allowed.has(argument))) {
  throw new Error('Usage: tsx scripts/import-large-vocabulary.ts [--dry-run|--apply]');
}
for (const key of ['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD'] as const) {
  if (!process.env[key]) throw new Error(`Missing ${key}`);
}
const database = process.env.DB_NAME!;
const host = process.env.DB_HOST!;
if (apply && !['localhost', '127.0.0.1', '::1'].includes(host)) throw new Error('M13 apply requires local PostgreSQL');
if (apply && !/^bivoca_m13_large_[a-z0-9_]+$/.test(database)) {
  throw new Error('M13 large import refused: DB_NAME must be an isolated bivoca_m13_large_* database');
}
const root = fileURLToPath(new URL('../', import.meta.url));
const approvedPath = new URL('../datasets/processed/bivoca_vocab_approved.jsonl', import.meta.url);
const topicsPath = new URL('../datasets/processed/bivoca_topics.json', import.meta.url);
const approvedText = await readFile(approvedPath, 'utf8');
const candidates = approvedText.trim().split(/\r?\n/).filter(Boolean).map(line => JSON.parse(line) as Candidate);
const topics = JSON.parse(await readFile(topicsPath, 'utf8')) as TopicInput[];
const approvedSha256 = createHash('sha256').update(approvedText, 'utf8').digest('hex');
const failAfter = Number(process.env.M13_FAIL_AFTER ?? '0');

for (const candidate of candidates) {
  if (candidate.validationStatus !== 'APPROVED_AUTO' || !candidate.importEligible ||
      !candidate.candidateId || !candidate.headword || !candidate.vietnamese ||
      !candidate.partOfSpeech || !['A1', 'A2', 'B1', 'B2'].includes(candidate.cefr) ||
      !candidate.primaryTopicCode || !candidate.topicCodes.includes(candidate.primaryTopicCode)) {
    throw new Error(`Invalid approved candidate ${candidate.candidateId || '<missing>'}`);
  }
}
const pool = new pg.Pool({ host, port: Number(process.env.DB_PORT), database,
  user: process.env.DB_USER, password: process.env.DB_PASSWORD, max: 2,
  connectionTimeoutMillis: 5000, statement_timeout: 30000, application_name: 'bivoca-m13-large-import' });
const started = performance.now();
try {
  assertDatabase: {
    const current = (await pool.query<{ name: string }>('SELECT current_database() AS name')).rows[0]?.name;
    if (current !== database) throw new Error('Connected database does not match DB_NAME');
    if (apply) {
      const required = await pool.query<{ ok: boolean }>(`SELECT
        to_regclass('public.vocabulary_sources') IS NOT NULL AND
        to_regclass('public.flashcard_provenance') IS NOT NULL AND
        to_regclass('public.flashcard_topics') IS NOT NULL AS ok`);
      if (required.rows[0]?.ok !== true) throw new Error('M13 staging migration is not applied');
    }
    break assertDatabase;
  }
  if (!apply) {
    console.log(JSON.stringify({ mode: 'dry-run', database, candidates: candidates.length,
      topicsReferenced: new Set(candidates.flatMap(candidate => candidate.topicCodes)).size,
      approvedSha256, durationMs: Math.round(performance.now() - started) }, null, 2));
  } else {
    const client = await pool.connect();
    let inserted = 0; let skipped = 0;
    try {
      await client.query('BEGIN');
      const referenced = new Set(candidates.flatMap(candidate => candidate.topicCodes));
      for (const topic of topics.filter(item => item.existing || referenced.has(item.code))) {
        await client.query(`INSERT INTO topics(code,name,description,image_url) VALUES($1,$2,$3,NULL)
          ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,
          updated_at=CURRENT_TIMESTAMP
          WHERE ROW(topics.name,topics.description) IS DISTINCT FROM ROW(EXCLUDED.name,EXCLUDED.description)`,
        [topic.code, topic.name, topic.description]);
      }
      const sources = [
        ['WORDS_CEFR_2024', 'Words CEFR Dataset', 'local-sha256:c5e38190',
          'https://github.com/Maximax67/Words-CEFR-Dataset', 'MIT',
          'c5e381905a87b47bd1321ac1cd34f870b107c0baa7b9f35f66e5b3a9b8ad8123'],
        ['SKYPEDIA_EN_VI_388ADC0', 'Skypedia English-Vietnamese Dictionary', '388adc0826300912e5b0311c089e0e70494b065f',
          'https://github.com/skypediacode/english-vietnamese-dictionary', 'CC BY-SA 4.0',
          '2b65bea9577c2a4149caa90940c806f321d1c6cfac503ed8bbd9f93e43a0033a'],
      ] as const;
      const sourceIds = new Map<string, number>();
      for (const source of sources) {
        const row = (await client.query<{ id: number }>(`INSERT INTO vocabulary_sources
          (code,name,version,source_url,license,checksum_sha256) VALUES($1,$2,$3,$4,$5,$6)
          ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name,version=EXCLUDED.version,
          source_url=EXCLUDED.source_url,license=EXCLUDED.license,checksum_sha256=EXCLUDED.checksum_sha256
          RETURNING id`, [...source])).rows[0];
        if (!row) throw new Error('Source upsert returned no row');
        sourceIds.set(source[0], row.id);
      }
      for (const candidate of candidates) {
        const dictionarySource = sourceIds.get('SKYPEDIA_EN_VI_388ADC0')!;
        const already = await client.query(`SELECT flashcard_id FROM flashcard_provenance
          WHERE source_id=$1 AND source_record_id=$2`, [dictionarySource, candidate.candidateId]);
        if (already.rowCount) { skipped += 1; continue; }
        const primary = (await client.query<{ id: number }>('SELECT id FROM topics WHERE code=$1',
          [candidate.primaryTopicCode])).rows[0];
        if (!primary) throw new Error(`Missing topic ${candidate.primaryTopicCode}`);
        const duplicate = await client.query<{ id: number }>(`SELECT id FROM flashcards
          WHERE LOWER(english)=LOWER($1::text COLLATE dataset_unicode)
            AND LOWER(vietnamese)=LOWER($2::text COLLATE dataset_unicode)`,
        [candidate.headword, candidate.vietnamese]);
        if (duplicate.rowCount) throw new Error(`Untracked duplicate candidate ${candidate.candidateId}`);
        const card = (await client.query<{ id: number }>(`INSERT INTO flashcards
          (topic_id,english,vietnamese,pronunciation,part_of_speech,example,example_vi,level,is_active)
          VALUES($1,$2,$3,$4,$5,$6,$7,$8,TRUE) RETURNING id`,
        [primary.id, candidate.headword, candidate.vietnamese, candidate.pronunciation,
          candidate.partOfSpeech, candidate.example, candidate.exampleVi, candidate.cefr])).rows[0];
        if (!card) throw new Error('Flashcard insert returned no row');
        for (const code of candidate.topicCodes) {
          const topic = (await client.query<{ id: number }>('SELECT id FROM topics WHERE code=$1', [code])).rows[0];
          if (!topic) throw new Error(`Missing related topic ${code}`);
          await client.query(`INSERT INTO flashcard_topics(flashcard_id,topic_id,is_primary)
            VALUES($1,$2,$3)`, [card.id, topic.id, code === candidate.primaryTopicCode]);
        }
        const provenanceRows = [
          [sourceIds.get('WORDS_CEFR_2024')!, `${candidate.candidateId}:cefr`, null, null],
          [dictionarySource, candidate.candidateId, candidate.source.wordId, candidate.source.definitionId],
        ] as const;
        for (const provenance of provenanceRows) {
          await client.query(`INSERT INTO flashcard_provenance
            (flashcard_id,source_id,source_record_id,source_word_id,source_definition_id,cefr_scope,
             validation_status,validation_reasons,quiz_eligible)
            VALUES($1,$2,$3,$4,$5,$6,'APPROVED_AUTO',$7::jsonb,$8)`,
          [card.id, ...provenance, candidate.cefrScope,
            JSON.stringify(candidate.validationReasons), candidate.quizEligible]);
        }
        inserted += 1;
        if (failAfter > 0 && inserted >= failAfter) throw new Error('M13_INJECTED_TRANSACTION_FAILURE');
      }
      await client.query('COMMIT');
      console.log(JSON.stringify({ mode: 'apply', database, approvedSha256,
        inserted, skipped, totalInput: candidates.length,
        durationMs: Math.round(performance.now() - started) }, null, 2));
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally { client.release(); }
  }
} catch (error) {
  const message = error instanceof Error ? error.message : 'Unknown M13 import error';
  const secret = process.env.DB_PASSWORD;
  console.error(secret ? message.replaceAll(secret, '[REDACTED]') : message);
  process.exitCode = 1;
} finally { await pool.end(); }
