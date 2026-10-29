import 'dotenv/config';
import { Pool } from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';
import { parseRoleArguments } from '../src/modules/admin/admin.schema.js';
import { createAdminRepository } from '../src/modules/admin/admin.repository.js';

// Explicit operator CLI only; never imported/called by server or registration.
let pool: Pool | undefined;
try {
  const input=parseRoleArguments(process.argv.slice(2));
  const env=readDatabaseEnv(process.env);
  pool=new Pool({host:env.DB_HOST,port:env.DB_PORT,database:env.DB_NAME,user:env.DB_USER,
    password:env.DB_PASSWORD,connectionTimeoutMillis:5000,max:1});
  const user=await createAdminRepository(pool).setRoleByEmail(input.email,input.role);
  if (!user) { console.error('Account not found; no account created.'); process.exitCode=1; }
  else console.log(`Role updated for user ID ${user.id}: ${user.role}`);
} catch {
  console.error('Role update failed. Check arguments (--email, --role USER|ADMIN), migration and database configuration.');
  process.exitCode=1;
} finally {
  if (pool) {
    try { await pool.end(); }
    catch { console.error('Database connection cleanup failed.'); process.exitCode=1; }
  }
}
