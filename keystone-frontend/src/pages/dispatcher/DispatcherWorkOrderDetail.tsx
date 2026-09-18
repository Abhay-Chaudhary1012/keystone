import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import type { AxiosError } from 'axios';
import { workOrdersApi } from '../../api/workOrders';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { useToast } from '../../components/common/Toast';
import { formatDateTime } from '../../utils/format';
import type { ApiErrorResponse, WorkOrder } from '../../types';

function apiErrorMessage(err: unknown, fallback: string): string {
  const axiosErr = err as AxiosError<ApiErrorResponse>;
  const data = axiosErr?.response?.data;
  if (data?.validationErrors?.length) return data.validationErrors.join('; ');
  return data?.message ?? fallback;
}

export function DispatcherWorkOrderDetail() {
  const { id } = useParams<{ id: string }>();
  const { addToast } = useToast();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [technicianId, setTechnicianId] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    workOrdersApi
      .get(Number(id))
      .then(setWo)
      .catch(() => setError('Failed to load work order.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <PageSpinner />;
  if (error || !wo) return <ErrorState message={error ?? 'Work order not found.'} />;

  const handleAssign = async () => {
    const techId = Number(technicianId);
    if (!techId || techId <= 0) {
      addToast('Enter a valid technician ID.', 'error');
      return;
    }
    setAssigning(true);
    try {
      const updated = await workOrdersApi.assign(wo.id, { technicianId: techId });
      setWo(updated);
      setTechnicianId('');
      addToast('Technician assigned successfully', 'success');
    } catch (err) {
      addToast(apiErrorMessage(err, 'Failed to assign technician'), 'error');
    } finally {
      setAssigning(false);
    }
  };

  const handleCancel = async () => {
    setCancelling(true);
    try {
      const updated = await workOrdersApi.cancel(wo.id);
      setWo(updated);
      addToast('Work order cancelled', 'info');
      setCancelDialogOpen(false);
    } catch (err) {
      addToast(apiErrorMessage(err, 'Failed to cancel work order'), 'error');
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-5">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500">
        <Link to="/dispatcher/work-orders" className="hover:text-blue-600">Work Orders</Link>
        {' / '}
        <span className="text-gray-800">#{wo.id}</span>
      </nav>

      {/* Header */}
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

      {/* Detail card */}
      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-800">Details</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-500">Customer</span>
            <p className="font-medium">{wo.customerName} ({wo.customerCode})</p>
          </div>
          <div>
            <span className="text-gray-500">Site</span>
            <p className="font-medium">{wo.siteName ?? '—'}</p>
          </div>
          <div>
            <span className="text-gray-500">Assigned Technician</span>
            <p className="font-medium">{wo.assignedTechnicianName ?? 'Unassigned'}</p>
          </div>
          <div>
            <span className="text-gray-500">Created By</span>
            <p className="font-medium">{wo.createdByName}</p>
          </div>
          <div>
            <span className="text-gray-500">Last Updated</span>
            <p className="font-medium">{formatDateTime(wo.updatedAt)}</p>
          </div>
        </div>

        <div>
          <span className="text-gray-500 text-sm">Description</span>
          <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">{wo.description}</p>
        </div>

        {wo.notes && (
          <div>
            <span className="text-gray-500 text-sm">Notes</span>
            <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">{wo.notes}</p>
          </div>
        )}
      </div>

      {/* Assign Technician — backend only allows this transition from OPEN
          (POST /work-orders/{id}/assign: OPEN -> ASSIGNED only). There is no
          technician directory endpoint, so the technician ID is entered
          directly rather than picked from a list; the backend validates
          that the ID belongs to an active TECHNICIAN. */}
      {wo.status === 'OPEN' && (
        <div className="card">
          <h2 className="font-semibold text-gray-800 mb-1">Assign Technician</h2>
          <p className="text-xs text-gray-400 mb-3">
            No technician directory endpoint exists yet — enter the technician's numeric user ID.
          </p>
          <div className="flex gap-3 flex-wrap">
            <input
              type="number"
              min={1}
              className="input sm:w-48"
              placeholder="Technician ID"
              value={technicianId}
              onChange={(e) => setTechnicianId(e.target.value)}
            />
            <button
              onClick={handleAssign}
              disabled={!technicianId || assigning}
              className="btn-primary"
            >
              {assigning ? 'Assigning…' : 'Assign'}
            </button>
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3 flex-wrap">
        <Link to={`/dispatcher/work-orders/${wo.id}/edit`} className="btn-secondary">
          ✏️ Edit
        </Link>
        {wo.status !== 'CANCELLED' && wo.status !== 'COMPLETED' && (
          <button onClick={() => setCancelDialogOpen(true)} className="btn-danger">
            Cancel Work Order
          </button>
        )}
      </div>

      <ConfirmDialog
        open={cancelDialogOpen}
        title="Cancel Work Order"
        message={`Are you sure you want to cancel "${wo.title}"? This action may be irreversible.`}
        confirmLabel="Yes, Cancel"
        onConfirm={handleCancel}
        onCancel={() => setCancelDialogOpen(false)}
        loading={cancelling}
      />
    </div>
  );
}
