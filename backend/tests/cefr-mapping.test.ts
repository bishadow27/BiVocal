import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { matchFlashcards, normalizeLexeme, parseCefrCsv, parseDelimited, summarizeMatches } from '../scripts/cefr/cefr-mapping.js';

const csv = `headword;pos;CEFR;CoreInventory 1;CoreInventory 2;Threshold\nbook;noun;A1;Objects;;\nbook;verb;A2;;;Services\nmarch;noun;A1;;;\nmarch;noun;B1;;;\n`;

describe('M13 CEFR mapping', () => {
  it('parses semicolon CSV including quotes and newlines', () => {
    assert.deepEqual(parseDelimited('a;b\n"x;y";"z""q"\n'), [['a', 'b'], ['x;y', 'z"q']]);
    assert.equal(parseCefrCsv(csv).length, 4);
  });

  it('normalizes Unicode, case and whitespace deterministically', () => {
    assert.equal(normalizeLexeme('  BOOK  '), 'book');
    assert.equal(normalizeLexeme('Ｔｅｓｔ'), 'test');
  });

  it('requires word and POS for an exact match and preserves unknowns', () => {
    const source = parseCefrCsv(csv);
    const matches = matchFlashcards([
      { id: 1, english: 'Book', partOfSpeech: 'noun', level: null },
      { id: 2, english: 'book', partOfSpeech: 'adjective', level: null },
      { id: 3, english: 'missing', partOfSpeech: 'noun', level: null },
      { id: 4, english: 'march', partOfSpeech: 'noun', level: 'B1' },
    ], source);
    assert.deepEqual(matches.map(match => [match.status, match.reason, match.sourceCefr]), [
      ['exact', 'EXACT_WORD_AND_POS', 'A1'],
      ['uncertain', 'WORD_MATCH_POS_MISMATCH', null],
      ['unmatched', 'HEADWORD_NOT_FOUND', null],
      ['uncertain', 'SOURCE_LEVEL_CONFLICT', null],
    ]);
    assert.deepEqual(summarizeMatches(matches).status, { exact: 1, uncertain: 2, unmatched: 1 });
  });

  it('marks same word and POS with multiple BiVoca senses as uncertain', () => {
    const source = parseCefrCsv(csv);
    const matches = matchFlashcards([
      { id: 1, english: 'book', partOfSpeech: 'noun', level: 'A1' },
      { id: 2, english: 'book', partOfSpeech: 'noun', level: 'A2' },
    ], source);
    assert.ok(matches.every(match => match.reason === 'MULTIPLE_BIVOCA_SENSES_FOR_WORD_AND_POS'));
  });
});
