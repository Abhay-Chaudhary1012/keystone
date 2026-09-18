import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { useAuth } from '../../context/AuthContext';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { PageSpinner } from '../../components/common/Spinner';
import { formatDate } from '../../utils/format';
import type { WorkOrder } from '../../types';

export function TechnicianDashboard() {
  const { user } = useAuth();
  const [assigned, setAssigned] = useState<WorkOrder[]>([]);
  const [inProgress, setInProgress] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    Promise.all([
      workOrdersApi.list({ status: 'ASSIGNED', size: 10 }),
      workOrdersApi.list({ status: 'IN_PROGRESS', size: 10 }),
    ])
      .then(([a, p]) => { setAssigned(a.content); setInProgress(p.content); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  if (loading) return <PageSpinner />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Good day, {user?.fullName}!
        </h1>
        <p className="text-sm text-gray-500 mt-1">Here's your work queue</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4">
        <div className="card border border-purple-200 bg-purple-50">
          <p className="text-3xl font-bold text-purple-700">{assigned.length}</p>
          <p className="text-sm font-medium text-purple-600 mt-1">Assigned to me</p>
        </div>
        <div className="card border border-yellow-200 bg-yellow-50">
          <p className="text-3xl font-bold text-yellow-700">{inProgress.length}</p>
          <p className="text-sm font-medium text-yellow-600 mt-1">In Progress</p>
        </div>
      </div>

      {/* In Progress */}
      {inProgress.length > 0 && (
        <div className="card">
          <h2 className="font-semibold text-gray-800 mb-3">🔧 Currently In Progress</h2>
          <div className="space-y-3">
            {inProgress.map((wo) => (
              <Link key={wo.id} to={`/technician/work-orders/${wo.id}`}
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">{wo.title}</p>
                  <p className="text-xs text-gray-400">{wo.customerName} · {formatDate(wo.createdAt)}</p>
                </div>
                <PriorityBadge priority={wo.priority} />
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Assigned */}
      <div className="card">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-semibold text-gray-800">📋 Assigned Work Orders</h2>
          <Link to="/technician/work-orders" className="text-sm text-blue-600 hover:underline">View all →</Link>
        </div>
        {assigned.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">No assigned work orders.</p>
        ) : (
          <div className="space-y-3">
            {assigned.map((wo) => (
              <Link key={wo.id} to={`/technician/work-orders/${wo.id}`}
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">{wo.title}</p>
                  <p className="text-xs text-gray-400">{wo.customerName} · {formatDate(wo.createdAt)}</p>
                </div>
                <StatusBadge status={wo.status} />
                <PriorityBadge priority={wo.priority} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
