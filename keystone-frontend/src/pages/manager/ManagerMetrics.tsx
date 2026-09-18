import { useEffect, useState } from 'react';
import { reportsApi } from '../../api/reports';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import type { ReportSummaryResponse, WorkOrderStatus } from '../../types';

interface StatusMeta {
  status: WorkOrderStatus;
  label: string;
  color: string;
  bg: string;
  getCount: (summary: ReportSummaryResponse) => number;
}

const STATUS_META: StatusMeta[] = [
  { status: 'OPEN', label: 'Open', color: 'text-blue-700', bg: 'bg-blue-500', getCount: (s) => s.openWorkOrders },
  { status: 'ASSIGNED', label: 'Assigned', color: 'text-purple-700', bg: 'bg-purple-500', getCount: (s) => s.assignedWorkOrders },
  { status: 'IN_PROGRESS', label: 'In Progress', color: 'text-yellow-700', bg: 'bg-yellow-500', getCount: (s) => s.inProgressWorkOrders },
  { status: 'ON_HOLD', label: 'On Hold', color: 'text-orange-700', bg: 'bg-orange-500', getCount: (s) => s.onHoldWorkOrders },
  { status: 'COMPLETED', label: 'Completed', color: 'text-green-700', bg: 'bg-green-500', getCount: (s) => s.completedWorkOrders },
  { status: 'CANCELLED', label: 'Cancelled', color: 'text-gray-600', bg: 'bg-gray-400', getCount: (s) => s.cancelledWorkOrders },
];

export function ManagerMetrics() {
  const [summary, setSummary] = useState<ReportSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    reportsApi
      .getSummary()
      .then(setSummary)
      .catch(() => setError('Failed to load operational metrics.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageSpinner />;
  if (error || !summary) return <ErrorState message={error ?? 'Metrics unavailable.'} />;

  const statusCounts = STATUS_META.map((meta) => ({ ...meta, count: meta.getCount(summary) }));
  const total = summary.totalWorkOrders;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Operational Metrics</h1>
        <p className="text-sm text-gray-500 mt-1">
          Live summary from the backend reporting API
        </p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card border border-blue-200 bg-blue-50">
          <p className="text-3xl font-bold text-blue-700">{summary.totalWorkOrders}</p>
          <p className="text-sm font-medium text-gray-600 mt-1">Total Work Orders</p>
        </div>
        <div className="card border border-green-200 bg-green-50">
          <p className="text-3xl font-bold text-green-700">{summary.completedWorkOrders}</p>
          <p className="text-sm font-medium text-gray-600 mt-1">Completed</p>
        </div>
        <div className="card border border-indigo-200 bg-indigo-50">
          <p className="text-3xl font-bold text-indigo-700">{summary.totalTechnicianMinutes}</p>
          <p className="text-sm font-medium text-gray-600 mt-1">Technician Minutes</p>
        </div>
        <div className="card border border-amber-200 bg-amber-50">
          <p className="text-3xl font-bold text-amber-700">${summary.totalPartsCost.toFixed(2)}</p>
          <p className="text-sm font-medium text-gray-600 mt-1">Parts Cost</p>
        </div>
      </div>

      <div className="card">
        <h2 className="font-semibold text-gray-800 mb-4">Work Orders by Status</h2>
        <div className="space-y-3">
          {statusCounts.map((s) => {
            const pct = total > 0 ? Math.round((s.count / total) * 100) : 0;
            return (
              <div key={s.status}>
                <div className="flex justify-between text-sm mb-1">
                  <span className={`font-medium ${s.color}`}>{s.label}</span>
                  <span className="text-gray-500">{s.count} ({pct}%)</span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-2">
                  <div className={`${s.bg} h-2 rounded-full transition-all`} style={{ width: `${pct}%` }} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="card border border-gray-200">
        <h2 className="font-semibold text-gray-800 mb-3">Current Reporting Scope</h2>
        <p className="text-sm text-gray-500">
          The backend currently provides work-order status counts, total logged parts cost,
          and total technician minutes. SLA compliance, average resolution time, cost per
          work order, and first-time-fix metrics are not exposed by the current API.
        </p>
      </div>
    </div>
  );
}
