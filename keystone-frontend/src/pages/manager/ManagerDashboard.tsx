import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { PageSpinner } from '../../components/common/Spinner';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import type { WorkOrder, WorkOrderStatus } from '../../types';

interface Counts { open: number; assigned: number; inProgress: number; onHold: number; completed: number; cancelled: number; }

export function ManagerDashboard() {
  const [counts, setCounts] = useState<Counts>({ open:0, assigned:0, inProgress:0, onHold:0, completed:0, cancelled:0 });
  const [recent, setRecent] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const statuses: [WorkOrderStatus, keyof Counts][] = [
      ['OPEN','open'], ['ASSIGNED','assigned'], ['IN_PROGRESS','inProgress'],
      ['ON_HOLD','onHold'], ['COMPLETED','completed'], ['CANCELLED','cancelled'],
    ];
    Promise.all([
      workOrdersApi.list({ size: 8, sort: 'createdAt,desc' }),
      ...statuses.map(([s]) => workOrdersApi.list({ status: s, size: 1 })),
    ]).then(([recentRes, ...statusRes]) => {
      setRecent(recentRes.content);
      const c: Counts = { open: 0, assigned: 0, inProgress: 0, onHold: 0, completed: 0, cancelled: 0 };
      statuses.forEach(([, key], i) => { c[key] = statusRes[i].totalElements; });
      setCounts(c);
    }).catch(() => {}).finally(() => setLoading(false));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) return <PageSpinner />;

  const total = counts.open + counts.assigned + counts.inProgress + counts.onHold + counts.completed + counts.cancelled;
  const completionRate = total > 0 ? Math.round((counts.completed / total) * 100) : 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Manager Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Operational overview</p>
        </div>
        <div className="flex gap-3">
          <Link to="/manager/kanban" className="btn-secondary">Kanban</Link>
          <Link to="/manager/work-orders" className="btn-primary">All Work Orders</Link>
        </div>
      </div>

      {/* Top stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Open',        value: counts.open,       color: 'bg-blue-50 border-blue-200',   text: 'text-blue-700'   },
          { label: 'In Progress', value: counts.inProgress, color: 'bg-yellow-50 border-yellow-200', text: 'text-yellow-700'},
          { label: 'On Hold',     value: counts.onHold,     color: 'bg-orange-50 border-orange-200', text: 'text-orange-700'},
          { label: 'Completed',   value: counts.completed,  color: 'bg-green-50 border-green-200',  text: 'text-green-700'  },
        ].map((s) => (
          <div key={s.label} className={`card border ${s.color}`}>
            <p className={`text-3xl font-bold ${s.text}`}>{s.value}</p>
            <p className="text-sm font-medium text-gray-600 mt-1">{s.label}</p>
          </div>
        ))}
      </div>

      {/* SLA / Metrics row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="card text-center">
          <p className="text-4xl font-bold text-blue-600">{completionRate}%</p>
          <p className="text-sm text-gray-500 mt-1">Completion Rate</p>
          <p className="text-xs text-gray-400">{counts.completed} of {total} total</p>
        </div>
        <div className="card text-center border border-dashed border-gray-200 bg-gray-50">
          <p className="text-4xl font-bold text-gray-300">—</p>
          <p className="text-sm text-gray-400 mt-1">Avg. Resolution Time</p>
          <p className="text-xs text-gray-400">SLA API coming soon</p>
        </div>
        <div className="card text-center border border-dashed border-gray-200 bg-gray-50">
          <p className="text-4xl font-bold text-gray-300">—</p>
          <p className="text-sm text-gray-400 mt-1">SLA Compliance</p>
          <p className="text-xs text-gray-400">SLA API coming soon</p>
        </div>
      </div>

      {/* Recent work orders */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-800">Recent Work Orders</h2>
          <Link to="/manager/work-orders" className="text-sm text-blue-600 hover:underline">View all →</Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="text-xs text-gray-400 uppercase border-b border-gray-100">
              <tr>
                <th className="pb-2 text-left">#</th>
                <th className="pb-2 text-left">Title</th>
                <th className="pb-2 text-left">Status</th>
                <th className="pb-2 text-left">Priority</th>
                <th className="pb-2 text-left hidden md:table-cell">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {recent.map((wo) => (
                <tr key={wo.id} className="hover:bg-gray-50">
                  <td className="py-2.5 text-gray-400 font-mono">#{wo.id}</td>
                  <td className="py-2.5">
                    <Link to={`/manager/work-orders/${wo.id}`}
                      className="font-medium text-gray-800 hover:text-blue-600 truncate block max-w-xs">
                      {wo.title}
                    </Link>
                  </td>
                  <td className="py-2.5"><StatusBadge status={wo.status} /></td>
                  <td className="py-2.5"><PriorityBadge priority={wo.priority} /></td>
                  <td className="py-2.5 hidden md:table-cell text-gray-500">{new Date(wo.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
