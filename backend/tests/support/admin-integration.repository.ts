import type { Pool } from 'pg';
import { createGamificationIntegrationRepository, type OwnedIdentity } from './gamification-integration.repository.js';

function ownedCode(code:string) {
  if (!/^ADM_[A-F0-9]{24}$/.test(code)) throw new Error('Invalid exact integration-owned topic code');
}
export function createAdminIntegrationRepository(pool:Pool) {
  return {
    ...createGamificationIntegrationRepository(pool),
    async adminPrerequisites() {
      const r=await pool.query<{present:boolean}>(`SELECT
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='users' AND column_name='role')
        AND to_regclass('topics') IS NOT NULL AND to_regclass('flashcards') IS NOT NULL
        AND to_regclass('users') IS NOT NULL AND to_regclass('user_progress') IS NOT NULL
        AND to_regclass('quiz_results') IS NOT NULL AND to_regclass('user_scores') IS NOT NULL
        AND to_regclass('xp_history') IS NOT NULL AND to_regclass('pgmigrations') IS NOT NULL
        AND to_regclass('idx_flashcards_unique_word_meaning') IS NOT NULL AS present`);
      return r.rows[0]?.present===true;
    },
    async promoteOwned(owner:OwnedIdentity) {
      const r=await pool.query("UPDATE users SET role='ADMIN' WHERE id=$1 AND username=$2 AND email=$3",[owner.id,owner.username,owner.email]);
      if(r.rowCount!==1)throw new Error('Exact owned admin user missing');
    },
    async ownedTopic(code:string) {
      ownedCode(code);return(await pool.query<{id:number}>('SELECT id FROM topics WHERE code=$1',[code])).rows[0];
    },
    async ownedCards(topicId:number,code:string) {
      ownedCode(code);return(await pool.query<{id:number}>(
        'SELECT f.id FROM flashcards f JOIN topics t ON t.id=f.topic_id WHERE t.id=$1 AND t.code=$2 ORDER BY f.id',[topicId,code])).rows.map(r=>r.id);
    },
    async deleteOwnedCard(id:number,topicId:number,code:string) {
      ownedCode(code);await pool.query(`DELETE FROM flashcards f USING topics t
        WHERE f.id=$1 AND f.topic_id=t.id AND t.id=$2 AND t.code=$3`,[id,topicId,code]);
    },
    async deleteOwnedTopic(id:number,code:string) {
      ownedCode(code);await pool.query('DELETE FROM topics WHERE id=$1 AND code=$2',[id,code]);
    },
    async baselineExcludingOwned(topicId:number,code:string) {
      ownedCode(code);
      const topic=await pool.query<{id:number}>('SELECT id FROM topics WHERE id=$1 AND code=$2',[topicId,code]);
      if(topic.rows.length!==1)throw new Error('Exact owned topic missing before baseline verification');
      // Full rows are intentionally read only in this integration repository for snapshot verification.
      const topics=await pool.query('SELECT * FROM topics WHERE id<>$1 ORDER BY id',[topicId]);
      const cards=await pool.query('SELECT * FROM flashcards WHERE topic_id<>$1 ORDER BY id',[topicId]);
      return JSON.stringify({topics:topics.rows,flashcards:cards.rows});
    },
  };
}
