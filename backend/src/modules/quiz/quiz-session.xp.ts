import type { PoolClient } from "pg";

/** v2 reference adapter; same 10/0, UTC-day dedup and streak policy as legacy.
 * Only the ledger reference differs. Caller owns the transaction. */
export async function awardSessionXp(
  client: PoolClient,
  userId: number,
  cardId: number,
  actionRowId: string,
  correct: boolean,
): Promise<number> {
  let awarded = 0;
  if (correct) {
    const inserted = await client.query<{ xp_amount: number }>(
      `INSERT INTO xp_history(user_id,flashcard_id,quiz_result_id,source_type,xp_amount,activity_date,quiz_action_id,quiz_action_operation)
 VALUES($1,$2,NULL,'QUIZ',10,(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,$3,'ANSWER')
 ON CONFLICT DO NOTHING RETURNING xp_amount`,
      [userId, cardId, actionRowId],
    );
    awarded = inserted.rows[0]?.xp_amount ?? 0;
  }
  const nextStreak = `CASE WHEN s.last_activity_date IS NULL THEN 1
 WHEN EXCLUDED.last_activity_date=s.last_activity_date+1 THEN s.current_streak+1
 WHEN EXCLUDED.last_activity_date>s.last_activity_date+1 THEN 1 ELSE s.current_streak END`;
  await client.query(
    `INSERT INTO user_scores AS s(user_id,total_xp,current_streak,longest_streak,last_activity_date)
 VALUES($1,$2,1,1,(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)
 ON CONFLICT(user_id) DO UPDATE SET total_xp=s.total_xp+EXCLUDED.total_xp,current_streak=${nextStreak},
 longest_streak=GREATEST(s.longest_streak,${nextStreak}),last_activity_date=GREATEST(s.last_activity_date,EXCLUDED.last_activity_date),updated_at=CURRENT_TIMESTAMP`,
    [userId, awarded],
  );
  return awarded;
}
