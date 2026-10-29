import jwt from 'jsonwebtoken';
import { AppError } from '../../shared/errors/app-error.js';

export interface TokenService {
  sign(userId: number, authVersion?: number): string;
  verify(token: string): number;
  verifySession(token: string): Promise<number>;
}

export function createTokenService(secret: string, expiresInSeconds: number,
  validateVersion?: (userId: number, authVersion: number) => Promise<boolean>): TokenService {
  const parse = (token: string): { userId: number; authVersion: number } => {
    try {
      const payload = jwt.verify(token, secret, { algorithms: ['HS256'] });
      if (typeof payload === 'string' || typeof payload.sub !== 'string' ||
          !/^[1-9]\d*$/.test(payload.sub) || !Number.isSafeInteger(Number(payload.sub)) ||
          Number(payload.sub) > 2147483647 || typeof payload.exp !== 'number' ||
          !Number.isInteger(payload.exp) || (payload.av !== undefined &&
          (typeof payload.av !== 'number' || !Number.isInteger(payload.av) || payload.av < 1))) {
        throw new Error('Invalid claims');
      }
      return { userId: Number(payload.sub), authVersion: payload.av ?? 1 };
    } catch { throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token'); }
  };
  return {
    sign(userId, authVersion = 1) {
      return jwt.sign({ av: authVersion }, secret, {
        algorithm: 'HS256', subject: String(userId), expiresIn: expiresInSeconds,
      });
    },
    verify(token) {
      return parse(token).userId;
    },
    async verifySession(token) {
      const claims = parse(token);
      if (validateVersion && !await validateVersion(claims.userId, claims.authVersion)) {
        throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
      }
      return claims.userId;
    },
  };
}
