import { Link } from 'react-router-dom';
import type { WorkOrder } from '../../types';
import { StatusBadge } from '../common/StatusBadge';
import { PriorityBadge } from '../common/PriorityBadge';
import { formatDate } from '../../utils/format';

interface Props {
  workOrders: WorkOrder[];
  basePath: string; // e.g. /dispatcher/work-orders
  onRowClick?: (wo: WorkOrder) => void;
}

export function WorkOrderTable({ workOrders, basePath }: Props) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
          <tr>
            <th className="px-4 py-3 text-left font-medium">#</th>
            <th className="px-4 py-3 text-left font-medium">Title</th>
            <th className="px-4 py-3 text-left font-medium">Status</th>
            <th className="px-4 py-3 text-left font-medium">Priority</th>
            <th className="px-4 py-3 text-left font-medium hidden md:table-cell">Customer</th>
            <th className="px-4 py-3 text-left font-medium hidden lg:table-cell">Technician</th>
            <th className="px-4 py-3 text-left font-medium hidden lg:table-cell">Updated</th>
            <th className="px-4 py-3 text-left font-medium">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {workOrders.map((wo) => (
            <tr key={wo.id} className="hover:bg-gray-50 transition-colors">
              <td className="px-4 py-3 text-gray-400 font-mono text-xs">#{wo.id}</td>
              <td className="px-4 py-3">
                <Link
                  to={`${basePath}/${wo.id}`}
                  className="font-medium text-gray-900 hover:text-blue-600 truncate block max-w-xs"
                >
                  {wo.title}
                </Link>
                <p className="text-xs text-gray-400 truncate max-w-xs">{wo.description}</p>
              </td>
              <td className="px-4 py-3">
                <StatusBadge status={wo.status} />
              </td>
              <td className="px-4 py-3">
                <PriorityBadge priority={wo.priority} />
              </td>
              <td className="px-4 py-3 hidden md:table-cell text-gray-600">
                {wo.customerName}
              </td>
              <td className="px-4 py-3 hidden lg:table-cell text-gray-600">
                {wo.assignedTechnicianName ?? (
                  <span className="text-gray-400 italic">Unassigned</span>
                )}
              </td>
              <td className="px-4 py-3 hidden lg:table-cell text-gray-500">
                {formatDate(wo.updatedAt)}
              </td>
              <td className="px-4 py-3">
                <Link
                  to={`${basePath}/${wo.id}`}
                  className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                >
                  View →
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
