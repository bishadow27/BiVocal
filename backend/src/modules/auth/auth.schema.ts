import { z } from 'zod';

const password = z.string().min(8).max(72)
  .refine((value) => Array.from(value).length >= 8)
  .refine((value) => Buffer.byteLength(value, 'utf8') <= 72);
const email = z.string().trim().max(150).email().transform((value) => value.toLowerCase());

export const registerSchema = z.object({
  username: z.string().trim().regex(/^[A-Za-z0-9_]{3,30}$/),
  email,
  password,
  fullName: z.string().trim().min(1).max(100).optional(),
}).strict();

export const loginSchema = z.object({ email, password }).strict();
export const googleAuthSchema = z.object({
  idToken: z.string().min(100).max(8192),
  challengeId: z.string().uuid(),
  nonce: z.string().min(32).max(256),
}).strict();
export const googleLinkSchema = googleAuthSchema.extend({ password });
export const forgotPasswordSchema = z.object({ email }).strict();
export const resetPasswordSchema = z.object({
  token: z.string().min(32).max(512),
  newPassword: password,
}).strict();
export type RegisterInput = z.infer<typeof registerSchema>;
export type LoginInput = z.infer<typeof loginSchema>;
export type GoogleAuthInput = z.infer<typeof googleAuthSchema>;
export type GoogleLinkInput = z.infer<typeof googleLinkSchema>;
export type ForgotPasswordInput = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordInput = z.infer<typeof resetPasswordSchema>;
