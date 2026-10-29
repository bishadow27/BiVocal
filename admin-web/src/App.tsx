import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './features/auth/AuthProvider';
import { ToastProvider } from './components/ui';
import { AppRoutes } from './routes/AppRoutes';
import { createQueryClient } from './lib/query';
const queryClient = createQueryClient();
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
