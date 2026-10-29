import { AppError } from '../../shared/errors/app-error.js';
import { createHash, randomBytes, randomUUID } from 'node:crypto';
import type { AuthRepository } from './auth.repository.js';
import type { ForgotPasswordInput, GoogleAuthInput, GoogleLinkInput, LoginInput,
  RegisterInput, ResetPasswordInput } from './auth.schema.js';
import type { PasswordService } from './password.service.js';
import type { TokenService } from './token.service.js';
import type { UserRow, UserDto, CurrentUserDto, AuthResponse } from './auth.types.js';
import type { GoogleTokenVerifier } from './google-token.service.js';
import type { EmailSender } from './email.service.js';

function toUser(row: UserRow): UserDto {
  return { id: row.id, username: row.username, email: row.email,
    fullName: row.full_name, avatarUrl: row.avatar_url };
}

function mapCreateError(error: unknown): never {
  if (typeof error === 'object' && error !== null && 'code' in error && error.code === '23505' &&
      'constraint' in error) {
    if (error.constraint === 'idx_users_email_unique') {
      throw new AppError(409, 'EMAIL_ALREADY_EXISTS', 'Email already exists');
    }
    if (error.constraint === 'idx_users_username_unique') {
      throw new AppError(409, 'USERNAME_ALREADY_EXISTS', 'Username already exists');
    }
  }
  throw error;
}

const digest = (value: string): string => createHash('sha256').update(value).digest('hex');
const unavailableGoogle: GoogleTokenVerifier = { async verify() {
  throw new AppError(503, 'GOOGLE_AUTH_NOT_CONFIGURED', 'Google Sign-In is not configured');
} };
const unavailableEmail: EmailSender = { configured: false, async sendPasswordReset() {
  throw new AppError(503, 'SMTP_CONFIGURATION_REQUIRED', 'Password recovery email is not configured');
} };

export interface AuthServiceOptions {
  googleVerifier?: GoogleTokenVerifier;
  emailSender?: EmailSender;
  publicResetUrl?: string;
}

export function createAuthService(repository: AuthRepository, passwords: PasswordService, tokens: TokenService,
  options: AuthServiceOptions = {}) {
  const google = options.googleVerifier ?? unavailableGoogle;
  const emailSender = options.emailSender ?? unavailableEmail;
  return {
    async register(input: RegisterInput): Promise<AuthResponse> {
      const passwordHash = await passwords.hash(input.password);
      let user: UserRow;
      try {
        user = await repository.create({ username: input.username, email: input.email,
          passwordHash, fullName: input.fullName ?? null });
      } catch (error: unknown) {
        mapCreateError(error);
      }
      return { user: toUser(user), accessToken: tokens.sign(user.id, user.auth_version ?? 1) };
    },
    async login(input: LoginInput): Promise<AuthResponse> {
      const user = await repository.findByEmail(input.email);
      if (!user) {
        await passwords.compareMissingUser(input.password);
        throw new AppError(401, 'INVALID_CREDENTIALS', 'Invalid email or password');
      }
      if (!user.password_hash || !await passwords.compare(input.password, user.password_hash)) {
        throw new AppError(401, 'INVALID_CREDENTIALS', 'Invalid email or password');
      }
      await repository.updateLastLogin(user.id);
      return { user: toUser(user), accessToken: tokens.sign(user.id, user.auth_version ?? 1) };
    },
    async currentUser(id: number): Promise<CurrentUserDto> {
      const user = await repository.findById(id);
      if (!user) throw new AppError(401, 'AUTH_USER_NOT_FOUND', 'Authenticated user not found');
      return { ...toUser(user), createdAt: user.created_at.toISOString() };
    },
    async isSessionValid(userId: number, authVersion: number): Promise<boolean> {
      if (!repository.findAuthVersion) return true;
      return await repository.findAuthVersion(userId) === authVersion;
    },
    async createGoogleChallenge(): Promise<{ challengeId: string; nonce: string; expiresInSeconds: number }> {
      if (!repository.createGoogleChallenge) throw new AppError(503, 'GOOGLE_AUTH_NOT_CONFIGURED', 'Google Sign-In is not configured');
      const challengeId = randomUUID();
      const nonce = randomBytes(32).toString('base64url');
      await repository.createGoogleChallenge(challengeId, digest(nonce), new Date(Date.now() + 5 * 60_000));
      return { challengeId, nonce, expiresInSeconds: 300 };
    },
    async googleLogin(input: GoogleAuthInput): Promise<AuthResponse> {
      if (!repository.consumeGoogleChallenge || !repository.loginWithGoogle) {
        throw new AppError(503, 'GOOGLE_AUTH_NOT_CONFIGURED', 'Google Sign-In is not configured');
      }
      const profile = await google.verify(input.idToken, input.nonce);
      if (!await repository.consumeGoogleChallenge(input.challengeId, digest(input.nonce))) {
        throw new AppError(401, 'GOOGLE_CHALLENGE_INVALID', 'Google authentication challenge is invalid or expired');
      }
      let usernameBase = profile.email.split('@')[0]?.replace(/[^A-Za-z0-9_]/g, '_').slice(0, 30) ?? '';
      if (!/^[A-Za-z0-9_]{3,30}$/.test(usernameBase)) usernameBase = `google_${profile.subject.slice(-12)}`;
      const result = await repository.loginWithGoogle(profile, usernameBase);
      if (result === 'EMAIL_LINK_REQUIRED') {
        throw new AppError(409, 'GOOGLE_ACCOUNT_LINK_REQUIRED',
          'This email already has a BiVoca account. Sign in with your password to link Google');
      }
      return { user: toUser(result), accessToken: tokens.sign(result.id, result.auth_version ?? 1) };
    },
    async linkGoogle(userId: number, input: GoogleLinkInput): Promise<AuthResponse> {
      if (!repository.consumeGoogleChallenge || !repository.linkGoogle) {
        throw new AppError(503, 'GOOGLE_AUTH_NOT_CONFIGURED', 'Google Sign-In is not configured');
      }
      const current = await repository.findById(userId);
      if (!current) throw new AppError(401, 'AUTH_USER_NOT_FOUND', 'Authenticated user not found');
      const credentials = await repository.findByEmail(current.email);
      if (!credentials?.password_hash || !await passwords.compare(input.password, credentials.password_hash)) {
        throw new AppError(401, 'LINK_REAUTH_FAILED', 'Password verification failed');
      }
      const profile = await google.verify(input.idToken, input.nonce);
      if (!await repository.consumeGoogleChallenge(input.challengeId, digest(input.nonce))) {
        throw new AppError(401, 'GOOGLE_CHALLENGE_INVALID', 'Google authentication challenge is invalid or expired');
      }
      const user = await repository.linkGoogle(userId, profile);
      return { user: toUser(user), accessToken: tokens.sign(user.id, user.auth_version ?? 1) };
    },
    async forgotPassword(input: ForgotPasswordInput): Promise<{ message: string }> {
      if (!emailSender.configured || !options.publicResetUrl || !repository.createPasswordReset) {
        throw new AppError(503, 'SMTP_CONFIGURATION_REQUIRED', 'Password recovery email is not configured');
      }
      const message = 'Nếu email thuộc tài khoản có mật khẩu, hướng dẫn đặt lại mật khẩu sẽ được gửi. Nếu bạn đăng ký bằng Google, hãy quay lại và chọn Tiếp tục với Google.';
      const user = await repository.findByEmail(input.email);
      if (!user?.password_hash) return { message };
      const rawToken = randomBytes(32).toString('base64url');
      await repository.createPasswordReset(user.id, digest(rawToken), new Date(Date.now() + 20 * 60_000));
      const resetUrl = new URL(options.publicResetUrl);
      resetUrl.searchParams.set('token', rawToken);
      await emailSender.sendPasswordReset(user.email, resetUrl.toString());
      return { message };
    },
    async resetPassword(input: ResetPasswordInput): Promise<{ message: string }> {
      if (!repository.resetPassword) throw new AppError(503, 'PASSWORD_RESET_NOT_CONFIGURED', 'Password reset is not configured');
      const passwordHash = await passwords.hash(input.newPassword);
      if (!await repository.resetPassword(digest(input.token), passwordHash)) {
        throw new AppError(400, 'RESET_TOKEN_INVALID', 'Password reset link is invalid or expired');
      }
      return { message: 'Mật khẩu đã được đặt lại thành công.' };
    },
  };
}

export type AuthService = ReturnType<typeof createAuthService>;
export interface AuthModule {
  service: AuthService;
  tokens: TokenService;
}
