import { Link } from 'react-router-dom';
import type { WorkOrder, WorkOrderStatus } from '../../types';
import { PriorityBadge } from '../common/PriorityBadge';

const COLUMNS: WorkOrderStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];
const COLUMN_LABELS: Record<WorkOrderStatus, string> = {
  OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In Progress',
  ON_HOLD: 'On Hold', COMPLETED: 'Completed', CANCELLED: 'Cancelled',
};
const COLUMN_COLORS: Record<WorkOrderStatus, string> = {
  OPEN:        'border-t-blue-400',
  ASSIGNED:    'border-t-purple-400',
  IN_PROGRESS: 'border-t-yellow-400',
  ON_HOLD:     'border-t-orange-400',
  COMPLETED:   'border-t-green-400',
  CANCELLED:   'border-t-gray-400',
};

interface Props {
  workOrders: WorkOrder[];
  basePath: string;
}

export function KanbanBoard({ workOrders, basePath }: Props) {
  const byStatus = COLUMNS.reduce<Record<WorkOrderStatus, WorkOrder[]>>(
    (acc, s) => ({ ...acc, [s]: workOrders.filter((wo) => wo.status === s) }),
    {} as Record<WorkOrderStatus, WorkOrder[]>,
  );

  return (
    <div className="overflow-x-auto pb-4">
      <div className="flex gap-4 min-w-max">
        {COLUMNS.map((col) => (
          <div key={col} className={`flex-shrink-0 w-72 bg-gray-100 rounded-xl border-t-4 ${COLUMN_COLORS[col]}`}>
            <div className="px-4 py-3 flex items-center justify-between">
              <h3 className="text-sm font-semibold text-gray-700">{COLUMN_LABELS[col]}</h3>
              <span className="text-xs bg-white rounded-full px-2 py-0.5 font-medium text-gray-500 shadow-sm">
                {byStatus[col].length}
              </span>
            </div>

            <div className="px-3 pb-3 space-y-2 min-h-[120px]">
              {byStatus[col].length === 0 ? (
                <div className="text-center py-8 text-gray-400 text-xs">No work orders</div>
              ) : (
                byStatus[col].map((wo) => (
                  <Link
                    key={wo.id}
                    to={`${basePath}/${wo.id}`}
                    className="block bg-white rounded-lg p-3 shadow-sm hover:shadow-md transition-shadow border border-gray-100 cursor-pointer"
                  >
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <span className="text-xs text-gray-400 font-mono">#{wo.id}</span>
                      <PriorityBadge priority={wo.priority} />
                    </div>
                    <p className="text-sm font-medium text-gray-800 line-clamp-2 mb-2">{wo.title}</p>
                    <p className="text-xs text-gray-500 truncate mb-1">📦 {wo.customerName}</p>
                    {wo.assignedTechnicianName ? (
                      <p className="text-xs text-gray-500">👤 {wo.assignedTechnicianName}</p>
                    ) : (
                      <p className="text-xs text-gray-400 italic">Unassigned</p>
                    )}
                  </Link>
                ))
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
