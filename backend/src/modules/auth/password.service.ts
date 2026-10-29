import bcrypt from 'bcrypt';
import { randomBytes } from 'node:crypto';

export interface PasswordService {
  hash(password: string): Promise<string>;
  compare(password: string, hash: string): Promise<boolean>;
  compareMissingUser(password: string): Promise<void>;
}

export function createPasswordService(): PasswordService {
  // Generated once on first missing-user login, used to avoid a fast existence check.
  let dummyHash: Promise<string> | undefined;
  return {
    hash: (password) => bcrypt.hash(password, 12),
    compare: (password, hash) => bcrypt.compare(password, hash),
    async compareMissingUser(password) {
      dummyHash ??= bcrypt.hash(randomBytes(32).toString('hex'), 12);
      await bcrypt.compare(password, await dummyHash);
    },
  };
}
