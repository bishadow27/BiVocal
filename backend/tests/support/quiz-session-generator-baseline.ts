import { randomInt, randomUUID, createHash } from "node:crypto";
import { AppError } from "../../src/shared/errors/app-error.js";

export type Method =
  "MIXED" | "CHALLENGE" | "CONTEXT" | "EN_TO_VI" | "VI_TO_EN" | "MATCHING";
export interface Card {
  id: number;
  english: string;
  vietnamese: string;
  example: string | null;
  example_vi?: string | null;
  eligible: boolean;
  topic_codes: string[];
  pronunciation: string | null;
  part_of_speech: string | null;
  level: string | null;
}
export interface Option {
  optionId: string;
  text: string;
}
export interface Generated {
  id: string;
  cardId: number;
  ordinal: number;
  kind: string;
  prompt: string;
  options: Option[];
  correctOptionId: string;
  roundIndex: number | null;
  roundPosition: number | null;
  metadata: Record<string, unknown>;
}
export const normalize = (s: string) =>
  s
    .normalize("NFKC")
    .trim()
    .toLocaleLowerCase("vi")
    .replace(/[“”‘’]/g, "'")
    .replace(/\s+/g, " ")
    .replace(/[.!?]+$/, "");
function shuffle<T>(values: readonly T[]): T[] {
  const a = [...values];
  for (let i = a.length - 1; i > 0; i--) {
    const j = randomInt(i + 1);
    [a[i], a[j]] = [a[j]!, a[i]!];
  }
  return a;
}
function cloze(card: Card): string | null {
  if (!card.example?.trim()) return null;
  const escaped = card.english.trim().replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const expression = new RegExp(
    "(?<![\\p{L}\\p{N}_])" + escaped + "(?![\\p{L}\\p{N}_])",
    "giu",
  );
  if ([...card.example.matchAll(expression)].length !== 1) return null;
  return card.example.replace(expression, "______");
}
function conflict(a: Card, b: Card): boolean {
  const parts = (s: string) =>
    new Set(
      normalize(s)
        .replace(/\([^)]*\)/g, " ")
        .split(/[,;/|]+/)
        .map((v) => v.trim())
        .filter(Boolean),
    );
  if (normalize(a.english) === normalize(b.english)) return true;
  const left = parts(a.vietnamese),
    right = parts(b.vietnamese);
  return [...left].some((v) => right.has(v));
}
export function preparePool(cards: Card[], method: Method): Card[] {
  const active = cards.filter(
    (c) => c.eligible && c.english.trim() && c.vietnamese.trim(),
  );
  const ens = new Map<string, number>(),
    vis = new Map<string, number>();
  for (const c of active) {
    ens.set(normalize(c.english), (ens.get(normalize(c.english)) ?? 0) + 1);
    vis.set(
      normalize(c.vietnamese),
      (vis.get(normalize(c.vietnamese)) ?? 0) + 1,
    );
  }
  const unique = active.filter(
    (c) =>
      ens.get(normalize(c.english)) === 1 &&
      vis.get(normalize(c.vietnamese)) === 1,
  );
  const context = ["CONTEXT", "MIXED", "CHALLENGE"].includes(method);
  const contextCounts = new Map<string, number>();
  for (const c of unique) {
    const prompt = cloze(c);
    if (prompt !== null) {
      const key = normalize(prompt);
      contextCounts.set(key, (contextCounts.get(key) ?? 0) + 1);
    }
  }
  let pool = unique.filter((c) => {
    const prompt = cloze(c);
    return (
      !context ||
      (prompt !== null && contextCounts.get(normalize(prompt)) === 1)
    );
  });
  if (method === "MATCHING")
    return pool.filter(
      (c) => !pool.some((other) => other.id !== c.id && conflict(c, other)),
    );
  if (method === "MIXED" || method === "CHALLENGE") {
    const owners = new Map<string, Set<number>>();
    for (const c of pool)
      for (const prompt of new Set([
        normalize(c.english),
        normalize(c.vietnamese),
        normalize(cloze(c)!),
      ])) {
        const ids = owners.get(prompt) ?? new Set<number>();
        ids.add(c.id);
        owners.set(prompt, ids);
      }
    pool = pool.filter((c) =>
      [
        normalize(c.english),
        normalize(c.vietnamese),
        normalize(cloze(c)!),
      ].every((prompt) => owners.get(prompt)!.size === 1),
    );
  }

  // Deterministic conservative pool: only retain candidates with three compatible distractors.
  // Iterate to a fixed point so displayed capacity remains valid after exclusions.
  let safe = pool;
  for (;;) {
    const next = safe.filter((c) => {
      const chosen: Card[] = [];
      for (const d of safe) {
        if (
          d.id !== c.id &&
          !conflict(c, d) &&
          chosen.every((v) => !conflict(v, d))
        )
          chosen.push(d);
        if (chosen.length === 3) return true;
      }
      return false;
    });
    if (next.length === safe.length) return next;
    safe = next;
  }
}
export function availability(cards: Card[], method: Method) {
  const pool = preparePool(cards, method);
  return {
    vocabularyCount: cards.length,
    quizEligibleCount: pool.length,
    unit: method === "MATCHING" ? "PAIR" : "QUESTION",
    enabledCounts: [20, 40, 60, 80, 100].filter((n) => n <= pool.length),
    fallbackUnitCount: pool.length > 0 && pool.length < 20 ? pool.length : null,
    availabilityVersion: createHash("sha256")
      .update(JSON.stringify({ method, cards }))
      .digest("hex"),
  };
}
export function generate(
  cards: Card[],
  method: Method,
  count: number,
): Generated[] {
  const pool = preparePool(cards, method);
  if (count < 1 || pool.length < count)
    throw new AppError(
      422,
      "INSUFFICIENT_ELIGIBLE",
      "Not enough eligible quiz units",
    );
  const selected = shuffle(pool).slice(0, count),
    positions = shuffle(Array.from({ length: count }, (_, i) => i % 4));
  const kinds = shuffle(
    Array.from(
      { length: count },
      (_, i) => ["CONTEXT", "EN_TO_VI", "VI_TO_EN"][i % 3]!,
    ),
  );
  const result: Generated[] = [];
  for (let i = 0; i < count; i++) {
    const c = selected[i]!,
      kind =
        method === "MATCHING"
          ? "MATCHING_PAIR"
          : method === "MIXED" || method === "CHALLENGE"
            ? kinds[i]!
            : method;
    let options: Option[], correctOptionId: string;
    if (kind === "MATCHING_PAIR") {
      const start = Math.floor(i / 4) * 4,
        round = selected.slice(start, start + 4);
      if (i % 4 === 0) {
        const shared = shuffle(
          round.map((card) => ({
            cardId: card.id,
            optionId: randomUUID(),
            text: card.vietnamese,
          })),
        );
        for (let j = 0; j < round.length; j++) {
          const card = round[j]!,
            answer = shared.find((o) => o.cardId === card.id)!;
          result.push({
            id: randomUUID(),
            cardId: card.id,
            ordinal: start + j,
            kind,
            prompt: card.english,
            options: shared.map(({ optionId, text }) => ({ optionId, text })),
            correctOptionId: answer.optionId,
            roundIndex: Math.floor(i / 4),
            roundPosition: j,
            metadata: {
              topicCodes: card.topic_codes.filter((v) => !v.startsWith("POS_")),
              posCodes: card.topic_codes.filter((v) => v.startsWith("POS_")),
              ipa: card.pronunciation,
            },
          });
        }
      }
      continue;
    }
    const distractors: Card[] = [];
    for (const d of shuffle(pool)) {
      if (
        d.id !== c.id &&
        !conflict(c, d) &&
        distractors.every((v) => !conflict(v, d))
      )
        distractors.push(d);
      if (distractors.length === 3) break;
    }
    // Random ordering could greedily select an incompatible trio; fall back to deterministic blueprint.
    if (distractors.length < 3) {
      distractors.length = 0;
      for (const d of pool) {
        if (
          d.id !== c.id &&
          !conflict(c, d) &&
          distractors.every((v) => !conflict(v, d))
        )
          distractors.push(d);
        if (distractors.length === 3) break;
      }
    }
    if (distractors.length !== 3)
      throw new AppError(
        422,
        "INSUFFICIENT_ELIGIBLE",
        "Distractor blueprint unavailable",
      );
    correctOptionId = randomUUID();
    const correct = {
      optionId: correctOptionId,
      text: kind === "EN_TO_VI" ? c.vietnamese : c.english,
    };
    options = shuffle(distractors).map((d) => ({
      optionId: randomUUID(),
      text: kind === "EN_TO_VI" ? d.vietnamese : d.english,
    }));
    options.splice(positions[i]!, 0, correct);
    result.push({
      id: randomUUID(),
      cardId: c.id,
      ordinal: i,
      kind,
      prompt:
        kind === "CONTEXT"
          ? cloze(c)!
          : kind === "VI_TO_EN"
            ? c.vietnamese
            : c.english,
      options,
      correctOptionId,
      roundIndex: null,
      roundPosition: null,
      metadata: {
        topicCodes: c.topic_codes.filter((v) => !v.startsWith("POS_")),
        posCodes: c.topic_codes.filter((v) => v.startsWith("POS_")),
        ipa: c.pronunciation,
      },
    });
  }
  return result;
}
