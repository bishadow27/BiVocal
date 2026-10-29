import nodemailer, { type SendMailOptions } from 'nodemailer';
import type SMTPTransport from 'nodemailer/lib/smtp-transport/index.js';
import { AppError } from '../../shared/errors/app-error.js';
import type { Env } from '../../config/env.js';

export interface EmailSender {
  readonly configured: boolean;
  sendPasswordReset(to: string, resetUrl: string): Promise<void>;
}

export type EmailTransportFactory = (options: SMTPTransport.Options) => {
  sendMail(options: SendMailOptions): Promise<unknown>;
};

const escapeHtml = (value: string): string => value.replace(/[&<>"']/g, (character) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character] ?? character);

export function createEmailSender(env: Env,
  transportFactory: EmailTransportFactory = (options) => nodemailer.createTransport(options)): EmailSender {
  const configured = Boolean(env.SMTP_HOST && env.SMTP_PORT && env.SMTP_USER &&
    env.SMTP_PASSWORD && env.MAIL_FROM && env.PUBLIC_RESET_URL);
  if (!configured) return {
    configured: false,
    async sendPasswordReset() {
      throw new AppError(503, 'SMTP_CONFIGURATION_REQUIRED', 'Password recovery email is not configured');
    },
  };
  const secure = env.SMTP_SECURE ?? false;
  const transport = transportFactory({ host: env.SMTP_HOST, port: env.SMTP_PORT!, secure,
    requireTLS: !secure, auth: { user: env.SMTP_USER!, pass: env.SMTP_PASSWORD! } });
  return {
    configured: true,
    async sendPasswordReset(to, resetUrl) {
      const safeUrl = escapeHtml(resetUrl);
      await transport.sendMail({ from: env.MAIL_FROM!, to, subject: 'BiVocal — Đặt lại mật khẩu',
        text: `Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản BiVocal của bạn.\n\nĐặt lại mật khẩu: ${resetUrl}\n\nLiên kết hết hạn sau 20 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.`,
        html: `<h2>BiVocal — Đặt lại mật khẩu</h2><p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản BiVocal của bạn.</p><p><a href="${safeUrl}">Đặt lại mật khẩu</a></p><p>Liên kết hết hạn sau 20 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>`,
      });
    },
  };
}
