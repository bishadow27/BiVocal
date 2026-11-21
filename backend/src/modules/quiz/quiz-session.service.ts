import type {
  QuizSessionRepository,
  CreateInput,
  AnswerInput,
  ControlInput,
  Filter,
} from "./quiz-session.repository.js";
import type { Method } from "./quiz-session.generator.js";
export function createQuizSessionService(repository: QuizSessionRepository) {
  return {
    availability: (method: Method, filter: Filter) =>
      repository.availability(method, filter),
    create: (user: number, input: CreateInput) =>
      repository.create(user, input),
    resume: (user: number, id: string) => repository.resume(user, id),
    questions: (user: number, id: string, cursor = 0, limit = 100) =>
      repository.questions(user, id, cursor, limit),
    answer: (user: number, id: string, input: AnswerInput) =>
      repository.answer(user, id, input),
    control: (
      user: number,
      id: string,
      operation: "COMPLETE" | "ABANDON",
      input: ControlInput,
    ) => repository.control(user, id, operation, input),
  };
}
export type QuizSessionService = ReturnType<typeof createQuizSessionService>;
