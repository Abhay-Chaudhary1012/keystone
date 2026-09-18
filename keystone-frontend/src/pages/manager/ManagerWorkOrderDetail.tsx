import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { formatDateTime } from '../../utils/format';
import type { WorkOrder } from '../../types';

export function ManagerWorkOrderDetail() {
  const { id } = useParams<{ id: string }>();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    workOrdersApi
      .get(Number(id))
      .then(setWo)
      .catch(() => setError('Failed to load work order.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <PageSpinner />;
  if (error || !wo) return <ErrorState message={error ?? 'Work order not found.'} />;

  return (
    <div className="max-w-3xl mx-auto space-y-5">
      <nav className="text-sm text-gray-500">
        <Link to="/manager/work-orders" className="hover:text-blue-600">
          Work Orders
        </Link>{' '}
        / <span className="text-gray-800">#{wo.id}</span>
      </nav>

      <div className="flex flex-col sm:flex-row sm:items-start gap-4">
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-gray-900">{wo.title}</h1>
          <p className="text-sm text-gray-500 mt-1">
            {wo.code} · Created {formatDateTime(wo.createdAt)}
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <StatusBadge status={wo.status} />
          <PriorityBadge priority={wo.priority} />
        </div>
      </div>

      <div className="card space-y-5">
        <h2 className="font-semibold text-gray-800">Details</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-500">Customer</span>
            <p className="font-medium mt-0.5">
              {wo.customerName} ({wo.customerCode})
            </p>
          </div>
          <div>
            <span className="text-gray-500">Site</span>
            <p className="font-medium mt-0.5">{wo.siteName ?? '—'}</p>
          </div>
          <div>
            <span className="text-gray-500">Assigned Technician</span>
            <p className="font-medium mt-0.5">
              {wo.assignedTechnicianName ?? 'Unassigned'}
            </p>
          </div>
          <div>
            <span className="text-gray-500">Created By</span>
            <p className="font-medium mt-0.5">{wo.createdByName}</p>
          </div>
          <div>
            <span className="text-gray-500">Last Updated</span>
            <p className="font-medium mt-0.5">{formatDateTime(wo.updatedAt)}</p>
          </div>
        </div>

        <div>
          <span className="text-sm text-gray-500">Description</span>
          <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">
            {wo.description}
          </p>
        </div>

        {wo.notes && (
          <div>
            <span className="text-sm text-gray-500">Notes</span>
            <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">
              {wo.notes}
            </p>
          </div>
        )}
      </div>

      {/* There is no endpoint that returns per-work-order time/cost totals —
          WorkOrderResponse doesn't include them, and there's no GET for
          parts/time entries. Only a global aggregate is available via
          GET /api/reports/summary (see Manager > Metrics). */}
      <div className="card border border-dashed border-gray-200 bg-gray-50 space-y-1">
        <p className="text-sm font-medium text-gray-500">
          ⏱ Time &amp; Cost Metrics
        </p>
        <p className="text-xs text-gray-400">
          Not available per work order — the backend only exposes an
          organization-wide total via Reports, not per-WO time/cost.
        </p>
      </div>
    </div>
  );
}
