import axios, { type AxiosError, type AxiosResponse } from 'axios';
import type { ApiErrorResponse } from '../types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';

export const apiClient = axios.create({
  baseURL: `${BASE_URL}/api`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

// ─── Request interceptor: attach JWT ─────────────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('ks_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Response interceptor: normalise errors ──────────────────────────────────
apiClient.interceptors.response.use(
  (res: AxiosResponse) => res,
  (err: AxiosError<ApiErrorResponse>) => {
    if (err.response?.status === 401) {
      // Token expired / invalid – clear and redirect to login
      localStorage.removeItem('ks_token');
      localStorage.removeItem('ks_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  },
);

export default apiClient;
