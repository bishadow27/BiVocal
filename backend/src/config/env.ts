import { z } from 'zod';

const port = z.string().regex(/^\d+$/).transform(Number).pipe(z.number().int().min(1).max(65535));
const databaseSchema = z.object({
  DB_HOST: z.string().trim().min(1),
  DB_PORT: port,
  DB_NAME: z.string().trim().min(1),
  DB_USER: z.string().trim().min(1),
  DB_PASSWORD: z.string().min(1),
});
const durationUnits: Record<string, number> = { s: 1, m: 60, h: 3600, d: 86400 };
const duration = z.string().regex(/^[1-9]\d*(s|m|h|d)$/).default('1h')
  .transform((value) => Number(value.slice(0, -1)) *
    (durationUnits[value.slice(-1)] ?? 0))
  .pipe(z.number().int().min(1).max(31536000));
const schema = databaseSchema.extend({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  HOST: z.string().trim().min(1).default('0.0.0.0'),
  PORT: port.default(3000),
  JWT_SECRET: z.string().min(32).max(4096).refine((value) => value.trim().length >= 32),
  JWT_EXPIRES_IN: duration,
  GOOGLE_WEB_CLIENT_ID: z.string().trim().regex(/^\d+-[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$/).optional(),
  SMTP_HOST: z.string().trim().min(1).optional(),
  SMTP_PORT: port.optional(),
  SMTP_USER: z.string().min(1).optional(),
  SMTP_PASSWORD: z.string().min(1).optional(),
  SMTP_SECURE: z.enum(['true', 'false']).transform((value) => value === 'true').optional(),
  MAIL_FROM: z.string().trim().min(1).optional(),
  PUBLIC_RESET_URL: z.string().url().optional(),
});

export type Env = z.infer<typeof schema>;
export type DatabaseEnv = z.infer<typeof databaseSchema>;

export function readDatabaseEnv(input: NodeJS.ProcessEnv): DatabaseEnv {
  const parsed = databaseSchema.safeParse(input);
  if (!parsed.success) {
    const fields = [...new Set(parsed.error.issues.map((issue) => issue.path.join('.')))];
    throw new Error(`Invalid or missing environment variables: ${fields.join(', ')}`);
  }
  return parsed.data;
}

export function readEnv(input: NodeJS.ProcessEnv): Env {
  const parsed = schema.safeParse(input);
  if (!parsed.success) {
    // Field names only: Zod errors or input values might expose credentials.
    const fields = [...new Set(parsed.error.issues.map((issue) => issue.path.join('.')))];
    throw new Error(`Invalid or missing environment variables: ${fields.join(', ')}`);
  }
  return parsed.data;
}
