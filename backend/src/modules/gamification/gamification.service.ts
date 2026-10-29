import type { GamificationRepository } from './gamification.repository.js';

export const levelForXp = (totalXp: number): number => Math.floor(totalXp / 100) + 1;
export function createGamificationService(repository: GamificationRepository) {
  return {
    async me(userId: number) {
      const score = await repository.findScore(userId);
      const totalXp = score?.total_xp ?? 0;
      return { totalXp, level: levelForXp(totalXp), xpIntoLevel: totalXp % 100, xpPerLevel: 100,
        currentStreak: score?.current_streak ?? 0, longestStreak: score?.longest_streak ?? 0,
        lastActivityDate: score?.last_activity_date ?? null };
    },
    async history(userId: number, limit: number) {
      return (await repository.history(userId, limit)).map((row) => ({ id: row.id, sourceType: row.source_type,
        flashcardId: row.flashcard_id, word: row.word, xpAmount: row.xp_amount,
        activityDate: row.activity_date, createdAt: row.created_at.toISOString() }));
    },
    async leaderboard(limit: number) {
      return (await repository.leaderboard(limit)).map((row) => {
        const rank = Number(row.rank);
        if (!Number.isSafeInteger(rank) || rank < 1) throw new Error('Invalid leaderboard rank');
        return { rank, userId: row.user_id, username: row.username, fullName: row.full_name,
          avatarUrl: row.avatar_url, totalXp: row.total_xp, level: levelForXp(row.total_xp),
          currentStreak: row.current_streak, longestStreak: row.longest_streak };
      });
    },
  };
}
export type GamificationService = ReturnType<typeof createGamificationService>;
