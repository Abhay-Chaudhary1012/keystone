import { useAuth } from '../../context/AuthContext';
import { initials } from '../../utils/format';

// GET /api/auth/me (verified against AuthController/AuthResponse) only
// returns: userId, username, email, fullName, role, customerId. There is no
// UserController and no PUT /api/users/{id} endpoint on the backend, so
// there is nothing to edit here and no other profile fields (phone, join
// date, etc.) to display — those don't exist in this API.
export function ProfilePage() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Profile</h1>

      <div className="card">
        <div className="flex items-center gap-5 mb-6">
          <div className="w-16 h-16 rounded-full bg-blue-600 flex items-center justify-center text-white text-xl font-bold">
            {initials(user.fullName)}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">{user.fullName}</h2>
            <p className="text-sm text-gray-500">{user.email}</p>
            <span className="badge bg-blue-100 text-blue-700 mt-1">{user.role}</span>
          </div>
        </div>

        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <dt className="text-xs font-medium text-gray-500 uppercase tracking-wide">Username</dt>
            <dd className="mt-1 text-sm text-gray-900">{user.username}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-gray-500 uppercase tracking-wide">Email</dt>
            <dd className="mt-1 text-sm text-gray-900">{user.email}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-gray-500 uppercase tracking-wide">Role</dt>
            <dd className="mt-1 text-sm text-gray-900">{user.role}</dd>
          </div>
          {user.customerId != null && (
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wide">Customer ID</dt>
              <dd className="mt-1 text-sm text-gray-900">#{user.customerId}</dd>
            </div>
          )}
        </dl>
      </div>

      <div className="card bg-amber-50 border-amber-200">
        <p className="text-sm text-amber-700">
          <strong>Note:</strong> Profile editing isn't available — the backend
          has no user-update endpoint (no <code>UserController</code>). This
          page shows exactly the fields the backend returns.
        </p>
      </div>
    </div>
  );
}
