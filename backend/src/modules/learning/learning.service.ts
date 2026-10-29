import type { LearningRepository } from './learning.repository.js';
import type { LearningCardDto } from './learning.types.js';

export function createLearningService(repository: LearningRepository) {
  return {
    async goal(userId:number) { return { dailyGoal: await repository.getGoal(userId) }; },
    async updateGoal(userId:number, dailyGoal:number) { return { dailyGoal: await repository.updateGoal(userId,dailyGoal) }; },
    async today(userId:number) { return repository.today(userId); },
    async history(userId:number, days:number) { return { days: await repository.history(userId,days) }; },
    async newCards(userId:number, limit:number, topicCode?:string, level?:string):Promise<LearningCardDto[]> {
      return (await repository.newCards(userId,limit,topicCode,level)).map(r=>({id:r.id,english:r.english,
        vietnamese:r.vietnamese,pronunciation:r.pronunciation,partOfSpeech:r.part_of_speech,
        example:r.example,exampleVi:r.example_vi,level:r.level}));
    },
  };
}
export type LearningService=ReturnType<typeof createLearningService>;
