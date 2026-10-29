import { OAuth2Client } from 'google-auth-library';
import { AppError } from '../../shared/errors/app-error.js';
import type { GoogleProfile } from './auth.types.js';

export interface GoogleTokenVerifier {
  verify(idToken: string, expectedNonce: string): Promise<GoogleProfile>;
}

export function createGoogleTokenVerifier(webClientId?: string): GoogleTokenVerifier {
  const client = new OAuth2Client();
  return {
    async verify(idToken, expectedNonce) {
      if (!webClientId) throw new AppError(503, 'GOOGLE_AUTH_NOT_CONFIGURED', 'Google Sign-In is not configured');
      let payload;
      try {
        const ticket = await client.verifyIdToken({ idToken, audience: webClientId });
        payload = ticket.getPayload();
      } catch (error) {
        if (error instanceof AppError) throw error;
        const reason = error instanceof Error ? error.message : '';
        if (reason.includes('Wrong recipient')) {
          throw new AppError(401, 'GOOGLE_AUDIENCE_INVALID', 'Google token audience is invalid');
        }
        if (reason.includes('Token used too early') || reason.includes('Token used too late')) {
          throw new AppError(401, 'GOOGLE_TOKEN_TIME_INVALID', 'Google token time is invalid');
        }
        throw new AppError(401, 'GOOGLE_TOKEN_INVALID', 'Google authentication failed');
      }
      if (!payload?.sub || !payload.email || payload.email_verified !== true) {
        throw new AppError(401, 'GOOGLE_CLAIMS_INVALID', 'Google token claims are invalid');
      }
      if (payload.nonce !== expectedNonce) {
        throw new AppError(401, 'GOOGLE_NONCE_MISMATCH', 'Google token nonce is invalid');
      }
      return { subject: payload.sub, email: payload.email.trim().toLowerCase(),
        fullName: typeof payload.name === 'string' ? payload.name.slice(0, 100) : null,
        pictureUrl: typeof payload.picture === 'string' ? payload.picture : null,
        nonce: expectedNonce };
    },
  };
}
