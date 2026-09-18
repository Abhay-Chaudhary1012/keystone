import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

const roleHome: Record<Role, string> = {
  DISPATCHER: '/dispatcher',
  TECHNICIAN: '/technician',
  MANAGER:    '/manager',
  CUSTOMER:   '/customer',
};

export function RoleRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={roleHome[user.role]} replace />;
}
