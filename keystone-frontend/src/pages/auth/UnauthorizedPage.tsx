import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export function UnauthorizedPage() {
  const { user } = useAuth();
  const home = user ? `/${user.role.toLowerCase()}` : '/login';
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="text-center max-w-md">
        <div className="text-6xl font-black text-gray-200 mb-4">403</div>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Access Denied</h1>
        <p className="text-gray-500 mb-6">
          You don't have permission to view this page.
        </p>
        <Link to={home} className="btn-primary">
          Go to Dashboard
        </Link>
      </div>
    </div>
  );
}
