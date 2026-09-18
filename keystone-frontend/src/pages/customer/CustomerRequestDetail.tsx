import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { formatDateTime } from '../../utils/format';
import type { WorkOrder, WorkOrderStatus } from '../../types';

// Visual progress tracker — ordered lifecycle steps visible to a customer
const TRACK_STEPS: { status: WorkOrderStatus; label: string; icon: string }[] = [
  { status: 'OPEN',        label: 'Request Received', icon: '📥' },
  { status: 'ASSIGNED',    label: 'Technician Assigned', icon: '👤' },
  { status: 'IN_PROGRESS', label: 'Work In Progress', icon: '🔧' },
  { status: 'COMPLETED',   label: 'Completed', icon: '✅' },
];

function getStepIndex(status: WorkOrderStatus): number {
  if (status === 'CANCELLED') return -1;
  if (status === 'ON_HOLD')   return 2; // stuck at in-progress
  const idx = TRACK_STEPS.findIndex((s) => s.status === status);
  return idx === -1 ? TRACK_STEPS.length - 1 : idx;
}

export function CustomerRequestDetail() {
  const { id } = useParams<{ id: string }>();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    workOrdersApi
      .get(Number(id))
      .then(setWo)
      .catch(() => setError('Failed to load request details.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <PageSpinner />;
  if (error || !wo) return <ErrorState message={error ?? 'Request not found.'} />;

  const stepIndex = getStepIndex(wo.status);

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500">
        <Link to="/customer/requests" className="hover:text-blue-600">
          My Requests
        </Link>{' '}
        / <span className="text-gray-800">#{wo.id}</span>
      </nav>

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start gap-3">
        <div className="flex-1">
          <h1 className="text-xl font-bold text-gray-900">{wo.title}</h1>
          <p className="text-xs text-gray-400 mt-1">
            Submitted {formatDateTime(wo.createdAt)}
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <StatusBadge status={wo.status} />
          <PriorityBadge priority={wo.priority} />
        </div>
      </div>

      {/* Status tracker */}
      {wo.status === 'CANCELLED' ? (
        <div className="card bg-red-50 border border-red-200">
          <p className="text-sm font-semibold text-red-700">
            ❌ This request has been cancelled.
          </p>
          <p className="text-xs text-red-500 mt-1">
            Please contact support if you believe this was an error.
          </p>
        </div>
      ) : (
        <div className="card">
          <h2 className="font-semibold text-gray-800 mb-5">Request Progress</h2>
          <div className="relative">
            {/* Connecting line */}
            <div className="absolute top-4 left-4 right-4 h-0.5 bg-gray-200" />
            <div
              className="absolute top-4 left-4 h-0.5 bg-blue-500 transition-all"
              style={{
                width:
                  stepIndex <= 0
                    ? '0%'
                    : `${(stepIndex / (TRACK_STEPS.length - 1)) * 100}%`,
              }}
            />
            <div className="relative flex justify-between">
              {TRACK_STEPS.map((step, idx) => {
                const done = idx <= stepIndex;
                const active = idx === stepIndex;
                return (
                  <div key={step.status} className="flex flex-col items-center">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center text-sm border-2 transition-all z-10 ${
                        done
                          ? 'bg-blue-600 border-blue-600 text-white'
                          : 'bg-white border-gray-300 text-gray-300'
                      } ${active ? 'ring-2 ring-blue-300 ring-offset-2' : ''}`}
                    >
                      {done ? '✓' : idx + 1}
                    </div>
                    <p
                      className={`mt-2 text-xs text-center max-w-[70px] leading-tight ${
                        done ? 'text-blue-700 font-medium' : 'text-gray-400'
                      }`}
                    >
                      {step.icon} {step.label}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>

          {wo.status === 'ON_HOLD' && (
            <div className="mt-5 p-3 bg-orange-50 border border-orange-200 rounded-lg">
              <p className="text-sm text-orange-700 font-medium">
                ⏸ Work is currently on hold.
              </p>
              <p className="text-xs text-orange-600 mt-0.5">
                A technician has been assigned but work is paused. We'll update
                you when it resumes.
              </p>
            </div>
          )}
        </div>
      )}

      {/* Details */}
      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-800">Details</h2>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-500">Submitted</span>
            <p className="font-medium mt-0.5">{formatDateTime(wo.createdAt)}</p>
          </div>
          <div>
            <span className="text-gray-500">Last Updated</span>
            <p className="font-medium mt-0.5">{formatDateTime(wo.updatedAt)}</p>
          </div>
          <div>
            <span className="text-gray-500">Assigned Technician</span>
            <p className="font-medium mt-0.5">
              {wo.assignedTechnicianName ?? 'Not yet assigned'}
            </p>
          </div>
          <div>
            <span className="text-gray-500">Site</span>
            <p className="font-medium mt-0.5">{wo.siteName ?? '—'}</p>
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
            <span className="text-sm text-gray-500">Technician Notes</span>
            <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">
              {wo.notes}
            </p>
          </div>
        )}
      </div>

      <div className="card bg-gray-50 border border-gray-200">
        <p className="text-xs text-gray-500">
          Need to update or escalate this request? Contact your account manager
          or call our support line. Reference request <strong>#{wo.id}</strong>.
        </p>
      </div>
    </div>
  );
}
