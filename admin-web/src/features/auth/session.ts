import type { AdminMe, LoginResponse } from '../../types/api';
import { ApiError, errorMessage } from '../../api/errors';

export const SESSION_KEY = 'bipassion.admin.session';
export interface Credentials {
  email: string;
  password: string;
}
export interface AuthApi {
  login: (input: Credentials, signal: AbortSignal) => Promise<LoginResponse>;
  me: (token: string, signal: AbortSignal) => Promise<AdminMe>;
}
export interface SessionState {
  accessToken: string | null;
  admin: AdminMe | null;
  isLoading: boolean;
  notice: string | null;
}
export class AuthSession {
  private state: SessionState;
  private listeners = new Set<() => void>();
  private endListeners = new Set<() => void>();
  private generation = 0;
  private request: AbortController | null = null;
  constructor(
    private api: AuthApi,
    private storage: Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> | null,
  ) {
    let token: string | null = null;
    try {
      token = storage?.getItem(SESSION_KEY) ?? null;
    } catch {
      /* memory-only session */
    }
    this.state = { accessToken: token, admin: null, isLoading: Boolean(token), notice: null };
  }
  getSnapshot = () => this.state;
  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };
  onEnd(listener: () => void) {
    this.endListeners.add(listener);
    return () => {
      this.endListeners.delete(listener);
    };
  }
  private publish(state: SessionState) {
    this.state = state;
    this.listeners.forEach((listener) => listener());
  }
  private save(token: string | null) {
    try {
      if (token) this.storage?.setItem(SESSION_KEY, token);
      else this.storage?.removeItem(SESSION_KEY);
    } catch {
      /* storage disabled */
    }
  }
  logout = (notice: string | null = null) => {
    this.generation++;
    this.request?.abort();
    this.request = null;
    this.save(null);
    this.endListeners.forEach((listener) => listener());
    this.publish({ accessToken: null, admin: null, isLoading: false, notice });
  };
  reject(token: string, error: ApiError) {
    if (token === this.state.accessToken) this.logout(errorMessage(error));
  }
  private checkAdmin(admin: AdminMe) {
    if (admin.role !== 'ADMIN')
      throw new ApiError(403, 'ADMIN_FORBIDDEN', 'Administrator access required');
  }
  async restore() {
    const token = this.state.accessToken;
    if (!token) return;
    const version = ++this.generation;
    this.request?.abort();
    this.request = new AbortController();
    this.publish({ ...this.state, isLoading: this.state.admin === null, notice: null });
    try {
      const admin = await this.api.me(token, this.request.signal);
      this.checkAdmin(admin);
      if (version === this.generation)
        this.publish({ accessToken: token, admin, isLoading: false, notice: null });
    } catch (error) {
      if (version !== this.generation) return;
      if (
        error instanceof ApiError &&
        (error.status === 401 || (error.status === 403 && error.code === 'ADMIN_FORBIDDEN'))
      ) {
        this.logout(errorMessage(error));
      } else {
        // A transient failure cannot prove that the stored session is invalid.
        this.publish({ ...this.state, isLoading: false, notice: errorMessage(error) });
      }
    }
  }
  async login(input: Credentials): Promise<boolean> {
    this.logout();
    const version = ++this.generation;
    this.request = new AbortController();
    this.publish({ accessToken: null, admin: null, isLoading: true, notice: null });
    try {
      const result = await this.api.login(input, this.request.signal);
      if (version !== this.generation) return false;
      const admin = await this.api.me(result.accessToken, this.request.signal);
      this.checkAdmin(admin);
      if (version !== this.generation) return false;
      this.save(result.accessToken);
      this.publish({ accessToken: result.accessToken, admin, isLoading: false, notice: null });
      return true;
    } catch (error) {
      if (version === this.generation) this.logout(errorMessage(error));
      return false;
    }
  }
}
