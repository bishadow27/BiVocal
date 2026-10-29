import { createHash } from 'node:crypto';

export const CEFR_HEADERS = [
  'headword', 'pos', 'CEFR', 'CoreInventory 1', 'CoreInventory 2', 'Threshold',
] as const;

export const CEFR_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const;
export type CefrLevel = typeof CEFR_LEVELS[number];
export type MatchStatus = 'exact' | 'uncertain' | 'unmatched';

export interface CefrRecord {
  sourceRow: number;
  headword: string;
  pos: string;
  cefr: CefrLevel;
  coreInventory1: string;
  coreInventory2: string;
  threshold: string;
}

export interface FlashcardInput {
  id: number;
  english: string;
  partOfSpeech: string | null;
  level: string | null;
}

export interface CefrMatch {
  flashcardId: number;
  english: string;
  partOfSpeech: string | null;
  previousLevel: string | null;
  status: MatchStatus;
  reason: string;
  sourceHeadword: string | null;
  sourcePos: string | null;
  sourceCefr: CefrLevel | null;
  candidateLevels: CefrLevel[];
  candidatePartsOfSpeech: string[];
  sourceTopics: string[];
}

export function normalizeLexeme(value: string | null | undefined): string {
  return (value ?? '').normalize('NFKC').trim().replace(/\s+/g, ' ').toLocaleLowerCase('en-US');
}

/** RFC 4180-style parser with a selectable one-character delimiter. */
export function parseDelimited(text: string, delimiter = ';'): string[][] {
  if (delimiter.length !== 1) throw new Error('Delimiter must be one character');
  const rows: string[][] = [];
  let row: string[] = [];
  let field = '';
  let quoted = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]!;
    if (quoted) {
      if (char === '"' && text[index + 1] === '"') { field += '"'; index += 1; }
      else if (char === '"') quoted = false;
      else field += char;
    } else if (char === '"' && field.length === 0) quoted = true;
    else if (char === delimiter) { row.push(field); field = ''; }
    else if (char === '\n') {
      row.push(field.replace(/\r$/, '')); rows.push(row); row = []; field = '';
    } else field += char;
  }
  if (quoted) throw new Error('Unclosed quoted CSV field');
  if (field.length > 0 || row.length > 0) { row.push(field.replace(/\r$/, '')); rows.push(row); }
  return rows;
}

export function parseCefrCsv(text: string): CefrRecord[] {
  const rows = parseDelimited(text.replace(/^\uFEFF/, ''), ';');
  const header = rows.shift();
  if (!header || header.length !== CEFR_HEADERS.length ||
      header.some((value, index) => value !== CEFR_HEADERS[index])) {
    throw new Error(`Invalid CEFR header; expected ${CEFR_HEADERS.join(';')}`);
  }
  const levels = new Set<string>(CEFR_LEVELS);
  return rows.map((columns, index) => {
    const sourceRow = index + 2;
    if (columns.length !== CEFR_HEADERS.length) throw new Error(`Invalid column count at row ${sourceRow}`);
    const [headword = '', pos = '', rawLevel = '', coreInventory1 = '', coreInventory2 = '', threshold = ''] = columns;
    const cefr = rawLevel.trim().toUpperCase();
    if (!headword.trim() || !pos.trim() || !levels.has(cefr)) {
      throw new Error(`Invalid required value at row ${sourceRow}`);
    }
    return { sourceRow, headword: headword.trim(), pos: pos.trim(), cefr: cefr as CefrLevel,
      coreInventory1: coreInventory1.trim(), coreInventory2: coreInventory2.trim(), threshold: threshold.trim() };
  });
}

const keyOf = (word: string, pos: string | null) => `${normalizeLexeme(word)}\u0000${normalizeLexeme(pos)}`;
const uniqueSorted = <T extends string>(values: Iterable<T>): T[] => [...new Set(values)].sort();

export function matchFlashcards(cards: FlashcardInput[], source: CefrRecord[]): CefrMatch[] {
  const byKey = new Map<string, CefrRecord[]>();
  const byWord = new Map<string, CefrRecord[]>();
  for (const record of source) {
    const key = keyOf(record.headword, record.pos);
    byKey.set(key, [...(byKey.get(key) ?? []), record]);
    const word = normalizeLexeme(record.headword);
    byWord.set(word, [...(byWord.get(word) ?? []), record]);
  }
  const cardKeyCounts = new Map<string, number>();
  for (const card of cards) {
    const key = keyOf(card.english, card.partOfSpeech);
    cardKeyCounts.set(key, (cardKeyCounts.get(key) ?? 0) + 1);
  }
  return cards.map(card => {
    const key = keyOf(card.english, card.partOfSpeech);
    const exactCandidates = byKey.get(key) ?? [];
    const wordCandidates = byWord.get(normalizeLexeme(card.english)) ?? [];
    const levels = uniqueSorted(exactCandidates.map(record => record.cefr));
    const candidatePartsOfSpeech = uniqueSorted(wordCandidates.map(record => record.pos));
    let status: MatchStatus;
    let reason: string;
    if (exactCandidates.length > 0 && levels.length > 1) {
      status = 'uncertain'; reason = 'SOURCE_LEVEL_CONFLICT';
    } else if (exactCandidates.length > 0 && (cardKeyCounts.get(key) ?? 0) > 1) {
      status = 'uncertain'; reason = 'MULTIPLE_BIVOCA_SENSES_FOR_WORD_AND_POS';
    } else if (exactCandidates.length > 0) {
      status = 'exact'; reason = exactCandidates.length > 1 ? 'EXACT_DUPLICATE_SOURCE_ROWS_SAME_LEVEL' : 'EXACT_WORD_AND_POS';
    } else if (wordCandidates.length > 0) {
      status = 'uncertain'; reason = 'WORD_MATCH_POS_MISMATCH';
    } else {
      status = 'unmatched'; reason = 'HEADWORD_NOT_FOUND';
    }
    const representative = status === 'exact' ? exactCandidates[0] : undefined;
    return {
      flashcardId: card.id, english: card.english, partOfSpeech: card.partOfSpeech,
      previousLevel: card.level, status, reason,
      sourceHeadword: representative?.headword ?? null,
      sourcePos: representative?.pos ?? null,
      sourceCefr: representative?.cefr ?? null,
      candidateLevels: levels,
      candidatePartsOfSpeech,
      sourceTopics: uniqueSorted(wordCandidates.flatMap(record =>
        [record.coreInventory1, record.coreInventory2, record.threshold].filter(Boolean))),
    };
  });
}

export function sourceDigest(text: string): string {
  return createHash('sha256').update(text, 'utf8').digest('hex');
}

export function summarizeSource(records: CefrRecord[]) {
  const count = (values: string[]) => Object.fromEntries(uniqueSorted(values).map(value =>
    [value, values.filter(candidate => candidate === value).length]));
  const keys = new Map<string, CefrRecord[]>();
  const headwords = new Map<string, number>();
  for (const record of records) {
    const key = keyOf(record.headword, record.pos);
    keys.set(key, [...(keys.get(key) ?? []), record]);
    const word = normalizeLexeme(record.headword);
    headwords.set(word, (headwords.get(word) ?? 0) + 1);
  }
  return {
    records: records.length,
    levels: count(records.map(record => record.cefr)),
    partsOfSpeech: count(records.map(record => record.pos)),
    duplicateKeys: [...keys.values()].filter(rows => rows.length > 1).length,
    duplicateExtraRows: [...keys.values()].reduce((sum, rows) => sum + Math.max(0, rows.length - 1), 0),
    conflictingLevelKeys: [...keys.values()].filter(rows => new Set(rows.map(row => row.cefr)).size > 1).length,
    headwordsWithMultipleRows: [...headwords.values()].filter(value => value > 1).length,
  };
}

export function summarizeMatches(matches: CefrMatch[]) {
  const status = { exact: 0, uncertain: 0, unmatched: 0 };
  const exactLevels: Record<string, number> = {};
  let levelConflicts = 0;
  for (const match of matches) {
    status[match.status] += 1;
    if (match.status === 'exact' && match.sourceCefr) {
      exactLevels[match.sourceCefr] = (exactLevels[match.sourceCefr] ?? 0) + 1;
      if (match.previousLevel !== null && match.previousLevel !== match.sourceCefr) levelConflicts += 1;
    }
  }
  return { total: matches.length, status, exactLevels, levelConflicts };
}
