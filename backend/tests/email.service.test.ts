import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import type SMTPTransport from 'nodemailer/lib/smtp-transport/index.js';
import type { Env } from '../src/config/env.js';
import { createEmailSender } from '../src/modules/auth/email.service.js';
import { AppError } from '../src/shared/errors/app-error.js';

const baseEnv = {
  NODE_ENV: 'test', HOST: '127.0.0.1', PORT: 3000,
  DB_HOST: '127.0.0.1', DB_PORT: 5432, DB_NAME: 'test', DB_USER: 'test', DB_PASSWORD: 'test',
  JWT_SECRET: 'email-test-secret-longer-than-thirty-two', JWT_EXPIRES_IN: 3600,
} satisfies Env;

describe('M14 EmailSender', () => {
  it('fails explicitly when SMTP is not configured', async () => {
    const sender = createEmailSender(baseEnv);
    assert.equal(sender.configured, false);
    await assert.rejects(() => sender.sendPasswordReset('user@example.com', 'https://example.com/reset'),
      (error: unknown) => error instanceof AppError && error.code === 'SMTP_CONFIGURATION_REQUIRED');
  });

  it('requires STARTTLS for port 587 and escapes the reset URL in HTML', async () => {
    let transportOptions: SMTPTransport.Options | undefined;
    let message: Record<string, unknown> | undefined;
    const sender = createEmailSender({ ...baseEnv, SMTP_HOST: 'smtp.gmail.com', SMTP_PORT: 587,
      SMTP_SECURE: false, SMTP_USER: 'sender@example.com', SMTP_PASSWORD: 'app-password',
      MAIL_FROM: 'BiVoca <sender@example.com>', PUBLIC_RESET_URL: 'http://127.0.0.1:3000/reset-password' },
    (options) => {
      transportOptions = options;
      return { async sendMail(value) { message = value as Record<string, unknown>; return {}; } };
    });

    await sender.sendPasswordReset('recipient@example.com',
      'http://127.0.0.1:3000/reset-password?token=a&next=<unsafe>');
    assert.equal(sender.configured, true);
    assert.equal(transportOptions?.secure, false);
    assert.equal(transportOptions?.requireTLS, true);
    assert.equal(message?.to, 'recipient@example.com');
    assert.match(String(message?.html), /token=a&amp;next=&lt;unsafe&gt;/);
    assert.match(String(message?.text), /token=a&next=<unsafe>/);
  });

  it('uses implicit TLS without STARTTLS on port 465', () => {
    let transportOptions: SMTPTransport.Options | undefined;
    const sender = createEmailSender({ ...baseEnv, SMTP_HOST: 'smtp.gmail.com', SMTP_PORT: 465,
      SMTP_SECURE: true, SMTP_USER: 'sender@example.com', SMTP_PASSWORD: 'app-password',
      MAIL_FROM: 'BiVoca <sender@example.com>', PUBLIC_RESET_URL: 'https://example.com/reset-password' },
    (options) => {
      transportOptions = options;
      return { async sendMail() { return {}; } };
    });
    assert.equal(sender.configured, true);
    assert.equal(transportOptions?.secure, true);
    assert.equal(transportOptions?.requireTLS, false);
  });
});
