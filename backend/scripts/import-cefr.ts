import 'dotenv/config';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { matchFlashcards, parseCefrCsv, sourceDigest, summarizeMatches, summarizeSource } from './cefr/cefr-mapping.js';

const SOURCE_URL = 'https://github.com/Maximax67/Words-CEFR-Dataset';
const SOURCE_LICENSE = 'MIT; Copyright (c) 2024 Belikov Maxim';
const datasetPath = fileURLToPath(new URL('../datasets/cefr/word_list_cefr.csv', import.meta.url));
const apply = process.argv.includes('--apply');
if (process.argv.some(arg => !['--apply', '--dry-run'].includes(arg) && arg !== process.argv[0] && arg !== process.argv[1])) {
  throw new Error('Usage: tsx scripts/import-cefr.ts [--dry-run|--apply]');
}

const required = ['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD'] as const;
for (const key of required) if (!process.env[key]) throw new Error(`Missing ${key}`);
const database = process.env.DB_NAME!;
const host = process.env.DB_HOST!;
if (apply && !['localhost', '127.0.0.1', '::1'].includes(host)) throw new Error('CEFR apply is restricted to local PostgreSQL');
if (apply && !/^bivoca_m13_[a-z0-9_]+$/.test(database)) {
  throw new Error('CEFR apply refused: DB_NAME must be an isolated bivoca_m13_* database');
}

const sourceText = await readFile(datasetPath, 'utf8');
const source = parseCefrCsv(sourceText);
const pool = new pg.Pool({ host, port: Number(process.env.DB_PORT), database,
  user: process.env.DB_USER, password: process.env.DB_PASSWORD, max: 2,
  connectionTimeoutMillis: 5000, statement_timeout: 10000, application_name: 'bivoca-m13-cefr' });
try {
  const current = await pool.query<{ name: string }>('SELECT current_database() AS name');
  if (current.rows[0]?.name !== database) throw new Error('Connected database does not match DB_NAME');
  const result = await pool.query<{ id: number; english: string; part_of_speech: string | null; level: string | null }>(
    'SELECT id, english, part_of_speech, level FROM flashcards ORDER BY id');
  const cards = result.rows.map(row => ({ id: row.id, english: row.english,
    partOfSpeech: row.part_of_speech, level: row.level }));
  const matches = matchFlashcards(cards, source);
  let auditRows = 0;
  let levelUpdates = 0;
  if (apply) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      await client.query(`CREATE TABLE IF NOT EXISTS cefr_flashcard_matches (
        flashcard_id INTEGER PRIMARY KEY REFERENCES flashcards(id) ON DELETE CASCADE,
        source_dataset TEXT NOT NULL,
        source_url TEXT NOT NULL,
        source_license TEXT NOT NULL,
        source_sha256 CHAR(64) NOT NULL,
        source_headword TEXT,
        source_pos TEXT,
        source_cefr VARCHAR(2),
        match_status TEXT NOT NULL CHECK (match_status IN ('exact','uncertain','unmatched')),
        match_reason TEXT NOT NULL,
        previous_level VARCHAR(10),
        candidate_levels JSONB NOT NULL,
        candidate_parts_of_speech JSONB NOT NULL,
        source_topics JSONB NOT NULL,
        imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
      )`);
      const digest = sourceDigest(sourceText);
      for (const match of matches) {
        await client.query(`INSERT INTO cefr_flashcard_matches AS existing
          (flashcard_id,source_dataset,source_url,source_license,source_sha256,source_headword,source_pos,
           source_cefr,match_status,match_reason,previous_level,candidate_levels,candidate_parts_of_speech,source_topics)
          VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12::jsonb,$13::jsonb,$14::jsonb)
          ON CONFLICT (flashcard_id) DO UPDATE SET
            source_dataset=EXCLUDED.source_dataset,source_url=EXCLUDED.source_url,
            source_license=EXCLUDED.source_license,source_sha256=EXCLUDED.source_sha256,
            source_headword=EXCLUDED.source_headword,source_pos=EXCLUDED.source_pos,source_cefr=EXCLUDED.source_cefr,
            match_status=EXCLUDED.match_status,match_reason=EXCLUDED.match_reason,
            candidate_levels=EXCLUDED.candidate_levels,candidate_parts_of_speech=EXCLUDED.candidate_parts_of_speech,
            source_topics=EXCLUDED.source_topics,imported_at=CURRENT_TIMESTAMP
          WHERE ROW(existing.source_dataset,existing.source_url,existing.source_license,existing.source_sha256,
            existing.source_headword,existing.source_pos,existing.source_cefr,existing.match_status,
            existing.match_reason,existing.candidate_levels,existing.candidate_parts_of_speech,existing.source_topics)
            IS DISTINCT FROM ROW(EXCLUDED.source_dataset,EXCLUDED.source_url,EXCLUDED.source_license,
            EXCLUDED.source_sha256,EXCLUDED.source_headword,EXCLUDED.source_pos,EXCLUDED.source_cefr,
            EXCLUDED.match_status,EXCLUDED.match_reason,EXCLUDED.candidate_levels,
            EXCLUDED.candidate_parts_of_speech,EXCLUDED.source_topics)`,
          [match.flashcardId, 'Words CEFR Dataset / word_list_cefr.csv', SOURCE_URL, SOURCE_LICENSE, digest,
            match.sourceHeadword, match.sourcePos, match.sourceCefr, match.status, match.reason,
            match.previousLevel, JSON.stringify(match.candidateLevels), JSON.stringify(match.candidatePartsOfSpeech),
            JSON.stringify(match.sourceTopics)]);
      }
      const updated = await client.query(`UPDATE flashcards AS f SET level=m.source_cefr, updated_at=CURRENT_TIMESTAMP
        FROM cefr_flashcard_matches AS m
        WHERE m.flashcard_id=f.id AND m.match_status='exact' AND m.source_cefr IS NOT NULL AND f.level IS NULL`);
      levelUpdates = updated.rowCount ?? 0;
      auditRows = Number((await client.query<{ count: string }>('SELECT count(*)::text AS count FROM cefr_flashcard_matches')).rows[0]?.count);
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally { client.release(); }
  }
  console.log(JSON.stringify({ mode: apply ? 'apply' : 'dry-run', database,
    sourceSha256: sourceDigest(sourceText), source: summarizeSource(source),
    matches: summarizeMatches(matches), auditRows, levelUpdates,
    unmatched: matches.filter(match => match.status === 'unmatched').map(match => match.english),
    uncertain: matches.filter(match => match.status === 'uncertain').map(match => ({ english: match.english, reason: match.reason })) }, null, 2));
} catch (error) {
  const secret = process.env.DB_PASSWORD;
  const message = error instanceof Error ? error.message : 'Unknown CEFR import failure';
  console.error(secret ? message.replaceAll(secret, '[REDACTED]') : message);
  process.exitCode = 1;
} finally { await pool.end(); }
