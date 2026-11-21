import { test } from "node:test";
import assert from "node:assert/strict";
import * as module from "../src/modules/quiz/quiz-session.generator.js";
const api = module as unknown as {
  preparePool?: (cards: unknown[], method: string) => unknown[];
  generate?: (
    cards: unknown[],
    method: string,
    count: number,
  ) => Array<{
    options: Array<{ optionId: string; text: string }>;
    correctOptionId: string;
    kind: string;
  }>;
};
test("generator balances option positions and MIXED kinds, never exposes grading in public DTO", () => {
  assert.equal(typeof api.generate, "function");
  const cards = Array.from({ length: 100 }, (_, i) => ({
    id: i + 1,
    english: "term" + i,
    vietnamese: "nghĩa" + i,
    example: "This is term" + i + " at location " + i + ".",
    eligible: true,
    topic_codes: ["TEST"],
    pronunciation: null,
    part_of_speech: "noun",
    level: "A1",
  }));
  const questions = api.generate!(cards, "MIXED", 100);
  assert.equal(questions.length, 100);
  const counts = [0, 0, 0, 0],
    kinds = new Map<string, number>();
  for (const q of questions) {
    const position = q.options.findIndex(
      (o) => o.optionId === q.correctOptionId,
    );
    assert.ok(position >= 0);
    counts[position]!++;
    kinds.set(q.kind, (kinds.get(q.kind) ?? 0) + 1);
  }
  assert.ok(Math.max(...counts) - Math.min(...counts) <= 1);
  assert.equal(kinds.has("MATCHING_PAIR"), false);
  assert.ok(Math.max(...kinds.values()) - Math.min(...kinds.values()) <= 1);
});
test("CONTEXT eligibility rejects missing or repeated source word; unverified cards excluded", () => {
  assert.equal(typeof api.preparePool, "function");
  const card = {
    id: 1,
    english: "term",
    vietnamese: "nghĩa",
    eligible: true,
    topic_codes: [],
    pronunciation: null,
    part_of_speech: "noun",
    level: "A1",
  };
  assert.equal(
    api.preparePool!([{ ...card, example: "Missing source word" }], "CONTEXT")
      .length,
    0,
  );
  assert.equal(
    api.preparePool!([{ ...card, example: "term term" }], "CONTEXT").length,
    0,
  );
  assert.equal(
    api.preparePool!(
      [{ ...card, eligible: false, example: "A term here" }],
      "EN_TO_VI",
    ).length,
    0,
  );
});

test("MATCHING excludes partial synonymous meanings from a shared answer column", () => {
  assert.equal(typeof api.preparePool, "function");
  const base = {
    eligible: true,
    topic_codes: [],
    pronunciation: null,
    part_of_speech: "noun",
    level: "A1",
    example: null,
  };
  const pool = api.preparePool!(
    [
      { ...base, id: 1, english: "cat", vietnamese: "mèo" },
      { ...base, id: 2, english: "feline", vietnamese: "mèo; họ mèo" },
    ],
    "MATCHING",
  );
  assert.equal(pool.length, 0);
});
test("CONTEXT removes repeated cloze prompts rather than only repeated English words", () => {
  assert.equal(typeof api.preparePool, "function");
  const cards = Array.from({ length: 8 }, (_, i) => ({
    id: i + 1,
    english: "word" + i,
    vietnamese: "meaning" + i,
    example:
      i < 2
        ? "I saw word" + i + "."
        : "I saw word" + i + " at location " + i + ".",
    eligible: true,
    topic_codes: [],
    pronunciation: null,
    part_of_speech: "noun",
    level: "A1",
  }));
  const pool = api.preparePool!(cards, "CONTEXT") as Array<{ id: number }>;
  assert.equal(
    pool.some((c) => c.id === 1 || c.id === 2),
    false,
  );
});

test("MIXED removes potential cross-kind prompt collisions", () => {
  const cards = Array.from({ length: 8 }, (_, i) => ({
    id: i + 1,
    english: "word" + i,
    vietnamese: i === 0 ? "word1" : "meaning" + i,
    example: "I saw word" + i + " at location " + i + ".",
    eligible: true,
    topic_codes: [],
    pronunciation: null,
    part_of_speech: "noun",
    level: "A1",
  }));
  const pool = api.preparePool!(cards, "MIXED") as Array<{ id: number }>;
  assert.equal(
    pool.some((c) => c.id === 1 || c.id === 2),
    false,
  );
});
