import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { PageSpinner } from '../../components/common/Spinner';
import { formatDate } from '../../utils/format';
import type { WorkOrder, WorkOrderStatus } from '../../types';

interface StatCard { label: string; value: number; color: string; status: WorkOrderStatus }

export function DispatcherDashboard() {
  const [recent, setRecent] = useState<WorkOrder[]>([]);
  const [counts, setCounts] = useState<Partial<Record<WorkOrderStatus, number>>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const statuses: WorkOrderStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD'];
    Promise.all([
      workOrdersApi.list({ size: 5, sort: 'createdAt,desc' }),
      ...statuses.map((s) => workOrdersApi.list({ status: s, size: 1 })),
    ]).then(([recentRes, ...statusRes]) => {
      setRecent(recentRes.content);
      const c: Partial<Record<WorkOrderStatus, number>> = {};
      statuses.forEach((s, i) => { c[s] = statusRes[i].totalElements; });
      setCounts(c);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  if (loading) return <PageSpinner />;

  const stats: StatCard[] = [
    { label: 'Open', value: counts.OPEN ?? 0, color: 'bg-blue-50 border-blue-200 text-blue-700', status: 'OPEN' },
    { label: 'Assigned', value: counts.ASSIGNED ?? 0, color: 'bg-purple-50 border-purple-200 text-purple-700', status: 'ASSIGNED' },
    { label: 'In Progress', value: counts.IN_PROGRESS ?? 0, color: 'bg-yellow-50 border-yellow-200 text-yellow-700', status: 'IN_PROGRESS' },
    { label: 'On Hold', value: counts.ON_HOLD ?? 0, color: 'bg-orange-50 border-orange-200 text-orange-700', status: 'ON_HOLD' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dispatcher Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Overview of active work orders</p>
        </div>
        <Link to="/dispatcher/work-orders/new" className="btn-primary">
          + New Work Order
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((s) => (
          <Link
            key={s.status}
            to={`/dispatcher/work-orders?status=${s.status}`}
            className={`card border ${s.color} hover:shadow-md transition-shadow`}
          >
            <p className="text-3xl font-bold">{s.value}</p>
            <p className="text-sm font-medium mt-1">{s.label}</p>
          </Link>
        ))}
      </div>

      {/* Recent Work Orders */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-800">Recent Work Orders</h2>
          <Link to="/dispatcher/work-orders" className="text-sm text-blue-600 hover:underline">
            View all →
          </Link>
        </div>
        {recent.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">No work orders yet.</p>
        ) : (
          <div className="space-y-3">
            {recent.map((wo) => (
              <Link
                key={wo.id}
                to={`/dispatcher/work-orders/${wo.id}`}
                className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">{wo.title}</p>
                  <p className="text-xs text-gray-400">{wo.customerName ?? `Customer #${wo.customerId}`} · {formatDate(wo.createdAt)}</p>
                </div>
                <StatusBadge status={wo.status} />
                <PriorityBadge priority={wo.priority} />
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Link to="/dispatcher/work-orders" className="card hover:shadow-md transition-shadow text-center">
          <div className="text-3xl mb-2">📋</div>
          <p className="font-medium text-gray-800">All Work Orders</p>
          <p className="text-xs text-gray-500 mt-1">Search, filter and manage</p>
        </Link>
        <Link to="/dispatcher/kanban" className="card hover:shadow-md transition-shadow text-center">
          <div className="text-3xl mb-2">📊</div>
          <p className="font-medium text-gray-800">Kanban Board</p>
          <p className="text-xs text-gray-500 mt-1">Visual status overview</p>
        </Link>
        <Link to="/dispatcher/work-orders/new" className="card hover:shadow-md transition-shadow text-center">
          <div className="text-3xl mb-2">➕</div>
          <p className="font-medium text-gray-800">New Work Order</p>
          <p className="text-xs text-gray-500 mt-1">Create a new request</p>
        </Link>
      </div>
    </div>
  );
}
