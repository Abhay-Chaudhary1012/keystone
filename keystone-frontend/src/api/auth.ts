import apiClient from './client';
import type { AuthResponse, LoginRequest } from '../types';

// Verified against AuthController: only POST /auth/login and GET /auth/me
// exist. There is no /auth/logout endpoint on the backend — logout is
// handled entirely client-side (clear the stored token) in AuthContext.

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),

  me: () =>
    apiClient.get<AuthResponse>('/auth/me').then((r) => r.data),
};
