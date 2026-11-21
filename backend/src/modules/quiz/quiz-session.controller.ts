import type { RequestHandler } from "express";
import { z } from "zod";
import { AppError } from "../../shared/errors/app-error.js";
import type { QuizSessionService } from "./quiz-session.service.js";
const uuid = z.uuid().transform((v) => v.toLowerCase());
const method = z.enum([
  "MIXED",
  "CHALLENGE",
  "CONTEXT",
  "EN_TO_VI",
  "VI_TO_EN",
  "MATCHING",
]);
const code = z.string().regex(/^[A-Z][A-Z0-9_]{0,49}$/);
const filter = z
  .object({
    topicCode: code.optional(),
    posCode: code.regex(/^POS_/).optional(),
  })
  .strict()
  .refine((v) => !(v.topicCode && v.posCode), "Choose topic OR POS");
export const createV2Schema = z
  .object({
    creationId: uuid,
    method,
    filter,
    selectedUnitCount: z.number().int().min(1).max(100),
    useAllAvailable: z.boolean().optional(),
    availabilityVersion: z
      .string()
      .regex(/^[0-9a-f]{64}$/)
      .optional(),
  })
  .strict()
  .refine(
    (v) =>
      v.useAllAvailable
        ? v.selectedUnitCount < 20
        : [20, 40, 60, 80, 100].includes(v.selectedUnitCount),
    "Invalid unit count",
  );
export const answerV2Schema = z
  .object({
    actionId: uuid,
    questionId: uuid,
    selectedOptionId: uuid,
    expectedVersion: z.number().int().min(0),
  })
  .strict();
export const controlV2Schema = z
  .object({ actionId: uuid, expectedVersion: z.number().int().min(0) })
  .strict();
const availabilitySchema = z
  .object({
    method,
    topicCode: code.optional(),
    posCode: code.regex(/^POS_/).optional(),
  })
  .strict()
  .refine((v) => !(v.topicCode && v.posCode));
const parse = <T>(schema: z.ZodType<T>, value: unknown): T => {
  const r = schema.safeParse(value);
  if (!r.success)
    throw new AppError(400, "VALIDATION_ERROR", "Invalid quiz session request");
  return r.data;
};
const questionsQuery = z
  .object({
    cursor: z
      .string()
      .regex(/^(0|[1-9]\d*)$/)
      .transform(Number)
      .pipe(z.number().int().min(0).max(100))
      .optional()
      .transform((v) => v ?? 0),
    limit: z
      .string()
      .regex(/^[1-9]\d*$/)
      .transform(Number)
      .pipe(z.number().int().min(1).max(100))
      .optional()
      .transform((v) => v ?? 100),
  })
  .strict();
export function createQuizSessionController(service: QuizSessionService) {
  const route =
    (
      handler: (
        user: number,
        id: string,
        body: unknown,
        query: unknown,
      ) => Promise<unknown>,
    ): RequestHandler =>
    async (req, res) => {
      const user: unknown = res.locals.userId;
      if (typeof user !== "number" || !Number.isInteger(user) || user < 1)
        throw new AppError(401, "AUTH_TOKEN_INVALID", "Invalid user");
      const id = req.params.id === undefined ? "" : parse(uuid, req.params.id);
      const result = await handler(user, id, req.body, req.query);
      if (
        result &&
        typeof result === "object" &&
        "receipt" in result &&
        "replayed" in result
      ) {
        const r = result as { receipt: unknown; replayed: boolean };
        res.setHeader("Idempotent-Replay", String(r.replayed));
        res
          .status(
            req.method === "POST" && req.path === "/sessions" && !r.replayed
              ? 201
              : 200,
          )
          .json(r.receipt);
      } else res.json(result);
    };
  return {
    availability: route(async (_user, _id, _body, query) => {
      const q = parse(availabilitySchema, query);
      return service.availability(q.method, {
        ...(q.topicCode ? { topicCode: q.topicCode } : {}),
        ...(q.posCode ? { posCode: q.posCode } : {}),
      });
    }),
    create: route((user, _id, body) =>
      service.create(user, parse(createV2Schema, body)),
    ),
    resume: route((user, id) => service.resume(user, id)),
    questions: route((user, id, _body, query) => {
      const q = parse(questionsQuery, query);
      return service.questions(user, id, q.cursor, q.limit);
    }),
    answer: route((user, id, body) =>
      service.answer(user, id, parse(answerV2Schema, body)),
    ),
    complete: route((user, id, body) =>
      service.control(user, id, "COMPLETE", parse(controlV2Schema, body)),
    ),
    abandon: route((user, id, body) =>
      service.control(user, id, "ABANDON", parse(controlV2Schema, body)),
    ),
  };
}
