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
  return axiosErr?.response?.data?.message ?? fallback;
}

type Action = 'start' | 'hold' | 'resume' | 'complete' | null;

const ACTION_CONFIG: Record<NonNullable<Action>, {
  label: string; title: string; message: string; variant: 'primary' | 'danger'; btnClass: string;
}> = {
  start:    { label: 'Start',    title: 'Start Work Order',    message: 'Mark this work order as In Progress?',   variant: 'primary', btnClass: 'btn-primary' },
  hold:     { label: 'On Hold',  title: 'Put On Hold',         message: 'Put this work order on hold?',           variant: 'danger',  btnClass: 'btn-danger' },
  resume:   { label: 'Resume',   title: 'Resume Work Order',   message: 'Resume work on this order?',             variant: 'primary', btnClass: 'btn-primary' },
  complete: { label: 'Complete', title: 'Complete Work Order', message: 'Mark this work order as completed?',     variant: 'primary', btnClass: 'btn-primary' },
};

export function TechnicianWorkOrderDetail() {
  const { id } = useParams<{ id: string }>();
  const { addToast } = useToast();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<Action>(null);
  const [acting, setActing] = useState(false);

  // Part logging form state
  const [partId, setPartId] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [loggingPart, setLoggingPart] = useState(false);

  // Time logging form state
  const [minutes, setMinutes] = useState('');
  const [timeNote, setTimeNote] = useState('');
  const [loggingTime, setLoggingTime] = useState(false);

  useEffect(() => {
    if (!id) return;
    workOrdersApi.get(Number(id))
      .then(setWo)
      .catch(() => setError('Failed to load work order.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <PageSpinner />;
  if (error || !wo) return <ErrorState message={error ?? 'Work order not found.'} />;

  const handleAction = async () => {
    if (!pendingAction || !wo) return;
    setActing(true);
    try {
      let updated: WorkOrder;
      switch (pendingAction) {
        case 'start':    updated = await workOrdersApi.start(wo.id);    break;
        case 'hold':     updated = await workOrdersApi.hold(wo.id);     break;
        case 'resume':   updated = await workOrdersApi.resume(wo.id);   break;
        case 'complete': updated = await workOrdersApi.complete(wo.id); break;
      }
      setWo(updated);
      addToast(`Work order ${pendingAction === 'complete' ? 'completed' : `${pendingAction}ed`} successfully`, 'success');
      setPendingAction(null);
    } catch {
      addToast(`Failed to ${pendingAction} work order`, 'error');
    } finally {
      setActing(false);
    }
  };

  // Determine available actions based on status
  const availableActions: Action[] = [];
  if (wo.status === 'ASSIGNED')    availableActions.push('start');
  if (wo.status === 'IN_PROGRESS') availableActions.push('hold', 'complete');
  if (wo.status === 'ON_HOLD')     availableActions.push('resume');

  // Backend enforces "technician must be assigned" + business-rule validation
  // server-side; restricting the forms to IN_PROGRESS here is UX-only, not a
  // substitute for that check.
  const canLogWork = wo.status === 'IN_PROGRESS';

  const handleLogPart = async (e: React.FormEvent) => {
    e.preventDefault();
    const qty = Number(quantity);
    const pid = Number(partId);
    if (!pid || pid <= 0 || !qty || qty <= 0) {
      addToast('Enter a valid part ID and a quantity greater than 0.', 'error');
      return;
    }
    setLoggingPart(true);
    try {
      await workOrdersApi.logPart(wo.id, { partId: pid, quantity: qty });
      setPartId('');
      setQuantity('1');
      addToast('Part logged successfully.', 'success');
    } catch (err) {
      addToast(apiErrorMessage(err, 'Failed to log part.'), 'error');
    } finally {
      setLoggingPart(false);
    }
  };

  const handleLogTime = async (e: React.FormEvent) => {
    e.preventDefault();
    const mins = Number(minutes);
    if (!mins || mins <= 0) {
      addToast('Enter a number of minutes greater than 0.', 'error');
      return;
    }
    setLoggingTime(true);
    try {
      await workOrdersApi.logTime(wo.id, { minutes: mins, notes: timeNote || undefined });
      setMinutes('');
      setTimeNote('');
      addToast('Time logged successfully.', 'success');
    } catch (err) {
      addToast(apiErrorMessage(err, 'Failed to log time.'), 'error');
    } finally {
      setLoggingTime(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      <nav className="text-sm text-gray-500">
        <Link to="/technician/work-orders" className="hover:text-blue-600">My Work Orders</Link>
        {' / '}
        <span className="text-gray-800">#{wo.id}</span>
      </nav>

      <div className="flex flex-col sm:flex-row sm:items-start gap-4">
        <div className="flex-1">
          <h1 className="text-xl font-bold text-gray-900">{wo.title}</h1>
          <p className="text-xs text-gray-400 mt-1">Created {formatDateTime(wo.createdAt)}</p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <StatusBadge status={wo.status} />
          <PriorityBadge priority={wo.priority} />
        </div>
      </div>

      {/* Detail */}
      <div className="card space-y-4">
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-500">Customer</span>
            <p className="font-medium">{wo.customerName ?? `#${wo.customerId}`}</p>
          </div>
          <div>
            <span className="text-gray-500">Site</span>
            <p className="font-medium">{wo.siteName ?? '—'}</p>
          </div>
          <div>
            <span className="text-gray-500">Scheduled</span>
            <p className="font-medium">{formatDateTime(wo.createdAt)}</p>
          </div>
          <div>
            <span className="text-gray-500">Priority</span>
            <p className="font-medium">{wo.priority}</p>
          </div>
        </div>

        <div>
          <span className="text-sm text-gray-500">Description</span>
          <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">{wo.description}</p>
        </div>

        {wo.notes && (
          <div>
            <span className="text-sm text-gray-500">Notes</span>
            <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">{wo.notes}</p>
          </div>
        )}
      </div>

      {/* Lifecycle Actions */}
      {availableActions.length > 0 && (
        <div className="card">
          <h2 className="font-semibold text-gray-800 mb-3">Actions</h2>
          <div className="flex gap-3 flex-wrap">
            {availableActions.map((action) => action && (
              <button
                key={action}
                onClick={() => setPendingAction(action)}
                className={ACTION_CONFIG[action].btnClass}
              >
                {ACTION_CONFIG[action].label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Time Logging — POST /work-orders/{id}/time (path confirmed; body fields unverified, see types/index.ts) */}
      <div className="card">
        <h2 className="font-semibold text-gray-800 mb-1">⏱ Time Logging</h2>
        {!canLogWork ? (
          <p className="text-xs text-gray-400">Time can be logged while this work order is In Progress.</p>
        ) : (
          <form onSubmit={handleLogTime} className="flex flex-col sm:flex-row gap-3 sm:items-end mt-2">
            <div className="flex-1">
              <label className="label" htmlFor="time-minutes">Minutes *</label>
              <input
                id="time-minutes"
                type="number"
                min={1}
                className="input"
                placeholder="e.g. 45"
                value={minutes}
                onChange={(e) => setMinutes(e.target.value)}
              />
            </div>
            <div className="flex-[2]">
              <label className="label" htmlFor="time-note">Note</label>
              <input
                id="time-note"
                type="text"
                className="input"
                placeholder="Optional note"
                value={timeNote}
                onChange={(e) => setTimeNote(e.target.value)}
              />
            </div>
            <button type="submit" disabled={loggingTime} className="btn-primary">
              {loggingTime ? 'Logging…' : 'Log Time'}
            </button>
          </form>
        )}
      </div>

      {/* Parts Used — POST /work-orders/{id}/parts (path confirmed; body fields unverified, see types/index.ts) */}
      <div className="card">
        <h2 className="font-semibold text-gray-800 mb-1">🔩 Parts Used</h2>
        {!canLogWork ? (
          <p className="text-xs text-gray-400">Parts can be logged while this work order is In Progress.</p>
        ) : (
          <form onSubmit={handleLogPart} className="flex flex-col sm:flex-row gap-3 sm:items-end mt-2">
            <div className="flex-1">
              <label className="label" htmlFor="part-id">Part ID *</label>
              <input
                id="part-id"
                type="number"
                min={1}
                className="input"
                placeholder="e.g. 12"
                value={partId}
                onChange={(e) => setPartId(e.target.value)}
              />
            </div>
            <div className="flex-1">
              <label className="label" htmlFor="part-qty">Quantity *</label>
              <input
                id="part-qty"
                type="number"
                min={1}
                className="input"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
              />
            </div>
            <button type="submit" disabled={loggingPart} className="btn-primary">
              {loggingPart ? 'Logging…' : 'Log Part'}
            </button>
          </form>
        )}
        <p className="mt-2 text-xs text-gray-400">
          Part ID must match an active part in inventory; the backend validates stock and quantity.
        </p>
      </div>

      {pendingAction && (
        <ConfirmDialog
          open
          title={ACTION_CONFIG[pendingAction].title}
          message={ACTION_CONFIG[pendingAction].message}
          confirmLabel={ACTION_CONFIG[pendingAction].label}
          variant={ACTION_CONFIG[pendingAction].variant}
          onConfirm={handleAction}
          onCancel={() => setPendingAction(null)}
          loading={acting}
        />
      )}
    </div>
  );
}
