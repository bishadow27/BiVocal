import { createContext, useContext, useEffect, useSyncExternalStore, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { session } from '../../api';
import type { AuthSession, Credentials, SessionState } from './session';

interface AuthState extends SessionState {
  isAuthenticated: boolean;
  login: (input: Credentials) => Promise<boolean>;
  logout: () => void;
  retryRestore: () => Promise<void>;
}
const AuthContext = createContext<AuthState | null>(null);
export function AuthProvider({
  children,
  store = session,
}: {
  children: ReactNode;
  store?: AuthSession;
}) {
  const queryClient = useQueryClient();
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot);
  useEffect(
    () =>
      store.onEnd(() => {
        void queryClient.cancelQueries();
        queryClient.clear();
      }),
    [store, queryClient],
  );
  useEffect(() => {
    void store.restore();
  }, [store]);
  return (
    <AuthContext.Provider
      value={{
        ...state,
        isAuthenticated: Boolean(state.accessToken && state.admin),
        login: (input) => store.login(input),
        logout: () => store.logout(),
        retryRestore: () => store.restore(),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
export function useAuth() {
  const auth = useContext(AuthContext);
  if (!auth) throw new Error('AuthProvider required');
  return auth;
}
