import { Link } from 'react-router-dom';
import { useWorkOrders } from '../../hooks/useWorkOrders';
import { Pagination } from '../../components/common/Pagination';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { EmptyState } from '../../components/common/EmptyState';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { WorkOrderFilters } from '../../components/workorders/WorkOrderFilters';
import { formatDate } from '../../utils/format';

export function CustomerRequests() {
  const { data, loading, error, filters, updateFilters, resetFilters, refresh } =
    useWorkOrders();

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">My Requests</h1>
        <Link to="/customer/requests/new" className="btn-primary">
          + New Request
        </Link>
      </div>

      <WorkOrderFilters
        filters={filters}
        onChange={updateFilters}
        onReset={resetFilters}
      />

      {loading ? (
        <PageSpinner />
      ) : error ? (
        <ErrorState message={error} onRetry={refresh} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="No requests found"
          description="You haven't submitted any service requests yet."
          action={
            <Link to="/customer/requests/new" className="btn-primary">
              Raise a Request
            </Link>
          }
        />
      ) : (
        <>
          {/* Mobile-friendly card list */}
          <div className="space-y-3 md:hidden">
            {data.content.map((wo) => (
              <Link
                key={wo.id}
                to={`/customer/requests/${wo.id}`}
                className="card block hover:shadow-md transition-shadow"
              >
                <div className="flex items-start justify-between gap-2 mb-2">
                  <span className="text-xs text-gray-400 font-mono">
                    #{wo.id}
                  </span>
                  <div className="flex gap-1">
                    <StatusBadge status={wo.status} />
                  </div>
                </div>
                <p className="text-sm font-semibold text-gray-800 mb-1">
                  {wo.title}
                </p>
                <div className="flex items-center justify-between text-xs text-gray-500 mt-2">
                  <span>{formatDate(wo.createdAt)}</span>
                  <PriorityBadge priority={wo.priority} />
                </div>
              </Link>
            ))}
          </div>

          {/* Desktop table */}
          <div className="hidden md:block overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">#</th>
                  <th className="px-4 py-3 text-left font-medium">Title</th>
                  <th className="px-4 py-3 text-left font-medium">Status</th>
                  <th className="px-4 py-3 text-left font-medium">Priority</th>
                  <th className="px-4 py-3 text-left font-medium">Submitted</th>
                  <th className="px-4 py-3 text-left font-medium">Submitted</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.content.map((wo) => (
                  <tr
                    key={wo.id}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td className="px-4 py-3 text-gray-400 font-mono text-xs">
                      #{wo.id}
                    </td>
                    <td className="px-4 py-3">
                      <Link
                        to={`/customer/requests/${wo.id}`}
                        className="font-medium text-gray-900 hover:text-blue-600"
                      >
                        {wo.title}
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={wo.status} />
                    </td>
                    <td className="px-4 py-3">
                      <PriorityBadge priority={wo.priority} />
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {formatDate(wo.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {formatDate(wo.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            onPageChange={(p) => updateFilters({ page: p })}
          />
        </>
      )}
    </div>
  );
}
