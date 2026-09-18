import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../components/common/Toast';
import { Spinner } from '../../components/common/Spinner';
import type { LoginRequest } from '../../types';

const schema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

export function LoginPage() {
  const { login, isAuthenticated, user } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginRequest>({
    resolver: zodResolver(schema),
  });

  if (isAuthenticated && user) {
    const roleHome: Record<string, string> = {
      DISPATCHER: '/dispatcher', TECHNICIAN: '/technician',
      MANAGER: '/manager', CUSTOMER: '/customer',
    };
    return <Navigate to={roleHome[user.role] ?? '/'} replace />;
  }

  const onSubmit = async (data: LoginRequest) => {
    try {
      await login(data);
      navigate('/');
    } catch {
      addToast('Invalid username or password. Please try again.', 'error');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-blue-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex w-14 h-14 bg-blue-500 rounded-2xl items-center justify-center text-white font-bold text-xl mb-3">
            K
          </div>
          <h1 className="text-2xl font-bold text-white">KEYSTONE</h1>
          <p className="text-gray-400 text-sm mt-1">Field Service Management</p>
        </div>

        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <h2 className="text-xl font-semibold text-gray-800 mb-6">Sign in to your account</h2>

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
            <div>
              <label className="label" htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                autoComplete="username"
                className={`input ${errors.username ? 'border-red-400 focus:ring-red-400' : ''}`}
                placeholder="dispatcher1"
                {...register('username')}
              />
              {errors.username && (
                <p className="mt-1 text-xs text-red-500">{errors.username.message}</p>
              )}
            </div>

            <div>
              <label className="label" htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                className={`input ${errors.password ? 'border-red-400 focus:ring-red-400' : ''}`}
                placeholder="••••••••"
                {...register('password')}
              />
              {errors.password && (
                <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary w-full h-11 text-base"
            >
              {isSubmitting ? (
                <span className="flex items-center gap-2 justify-center">
                  <Spinner size="sm" className="text-white" /> Signing in…
                </span>
              ) : 'Sign in'}
            </button>
          </form>
        </div>

        <p className="text-center text-gray-500 text-xs mt-6">
          © {new Date().getFullYear()} Keystone FSM. All rights reserved.
        </p>
      </div>
    </div>
  );
}
