import { Router, json } from "express";
import { authenticate } from "../../shared/middleware/authenticate.js";
import type { TokenService } from "../auth/token.service.js";
import type { QuizSessionRepository } from "./quiz-session.repository.js";
import { createQuizSessionService } from "./quiz-session.service.js";
import { createQuizSessionController } from "./quiz-session.controller.js";
export function createQuizSessionRouter(
  repository: QuizSessionRepository,
  tokens: TokenService,
  ready?: () => Promise<void>,
) {
  const router = Router(),
    c = createQuizSessionController(createQuizSessionService(repository));
  router.use(
    ["/availability", "/sessions"],
    authenticate(tokens),
    async (_req, _res, next) => { if (ready) await ready(); next(); },
    json({ limit: "16kb" }),
  );
  router.get("/availability", c.availability);
  router.post("/sessions", c.create);
  router.get("/sessions/:id", c.resume);
  router.get("/sessions/:id/questions", c.questions);
  router.post("/sessions/:id/answers", c.answer);
  router.post("/sessions/:id/complete", c.complete);
  router.post("/sessions/:id/abandon", c.abandon);
  return router;
}
