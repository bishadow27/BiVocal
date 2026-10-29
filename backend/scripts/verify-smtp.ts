import 'dotenv/config';
import nodemailer from 'nodemailer';
import { readEnv } from '../src/config/env.js';

const env = readEnv(process.env);
if (!env.SMTP_HOST || !env.SMTP_PORT || !env.SMTP_USER || !env.SMTP_PASSWORD || !env.MAIL_FROM ||
    !env.PUBLIC_RESET_URL) {
  throw new Error('SMTP_CONFIGURATION_REQUIRED');
}

const secure = env.SMTP_SECURE ?? false;
const transport = nodemailer.createTransport({ host: env.SMTP_HOST, port: env.SMTP_PORT, secure,
  requireTLS: !secure, auth: { user: env.SMTP_USER, pass: env.SMTP_PASSWORD } });

try {
  await transport.verify();
  console.log('SMTP_VERIFY_OK');
} catch (error: unknown) {
  const safe = typeof error === 'object' && error !== null ? error as {
    code?: unknown; command?: unknown; responseCode?: unknown;
  } : {};
  console.error(JSON.stringify({ status: 'SMTP_VERIFY_FAILED',
    code: String(safe.code ?? 'UNKNOWN'), command: String(safe.command ?? 'UNKNOWN'),
    responseCode: typeof safe.responseCode === 'number' ? safe.responseCode : null }));
  process.exitCode = 1;
} finally {
  transport.close();
}
