import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { useAuth } from '../../context/AuthContext';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { PageSpinner } from '../../components/common/Spinner';
import { formatDate } from '../../utils/format';
import type { WorkOrder, WorkOrderStatus } from '../../types';

export function CustomerDashboard() {
  const { user } = useAuth();
  const [recent, setRecent] = useState<WorkOrder[]>([]);
  const [counts, setCounts] = useState<Partial<Record<WorkOrderStatus, number>>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    // Filter by customer — uses customerId once the backend wires it to the
    // authenticated customer's profile. For now pass the user id; update when
    // customer-profile mapping is available on the backend.
    const activeStatuses: WorkOrderStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED'];
    Promise.all([
      workOrdersApi.list({ size: 5, sort: 'createdAt,desc' }),
      ...activeStatuses.map((s) =>
        workOrdersApi.list({ status: s, size: 1 }),
      ),
    ])
      .then(([recentRes, ...statusRes]) => {
        setRecent(recentRes.content);
        const c: Partial<Record<WorkOrderStatus, number>> = {};
        activeStatuses.forEach((s, i) => {
          c[s] = statusRes[i].totalElements;
        });
        setCounts(c);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  if (loading) return <PageSpinner />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Welcome, {user?.fullName}!
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Track your service requests and work orders
          </p>
        </div>
        <Link to="/customer/requests/new" className="btn-primary">
          + New Request
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Open',        value: counts.OPEN ?? 0,        color: 'bg-blue-50 border-blue-200 text-blue-700'     },
          { label: 'Assigned',    value: counts.ASSIGNED ?? 0,    color: 'bg-purple-50 border-purple-200 text-purple-700'},
          { label: 'In Progress', value: counts.IN_PROGRESS ?? 0, color: 'bg-yellow-50 border-yellow-200 text-yellow-700'},
          { label: 'Completed',   value: counts.COMPLETED ?? 0,   color: 'bg-green-50 border-green-200 text-green-700'  },
        ].map((s) => (
          <div key={s.label} className={`card border ${s.color}`}>
            <p className="text-3xl font-bold">{s.value}</p>
            <p className="text-sm font-medium mt-1">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Recent requests */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-800">Recent Requests</h2>
          <Link
            to="/customer/requests"
            className="text-sm text-blue-600 hover:underline"
          >
            View all →
          </Link>
        </div>

        {recent.length === 0 ? (
          <div className="text-center py-10">
            <p className="text-gray-400 text-sm mb-3">
              No service requests yet.
            </p>
            <Link to="/customer/requests/new" className="btn-primary">
              Raise a Request
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {recent.map((wo) => (
              <Link
                key={wo.id}
                to={`/customer/requests/${wo.id}`}
                className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">
                    {wo.title}
                  </p>
                  <p className="text-xs text-gray-400">
                    {formatDate(wo.createdAt)}
                  </p>
                </div>
                <StatusBadge status={wo.status} />
                <PriorityBadge priority={wo.priority} />
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Quick links */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Link
          to="/customer/requests"
          className="card hover:shadow-md transition-shadow"
        >
          <div className="text-3xl mb-2">📋</div>
          <p className="font-medium text-gray-800">My Requests</p>
          <p className="text-xs text-gray-500 mt-1">
            View and track all your service requests
          </p>
        </Link>
        <Link
          to="/customer/requests/new"
          className="card hover:shadow-md transition-shadow"
        >
          <div className="text-3xl mb-2">➕</div>
          <p className="font-medium text-gray-800">New Request</p>
          <p className="text-xs text-gray-500 mt-1">
            Submit a new maintenance request
          </p>
        </Link>
      </div>
    </div>
  );
}
