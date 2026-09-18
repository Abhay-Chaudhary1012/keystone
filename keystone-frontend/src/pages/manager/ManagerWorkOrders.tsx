import { Link } from 'react-router-dom';
import { useWorkOrders } from '../../hooks/useWorkOrders';
import { WorkOrderTable } from '../../components/workorders/WorkOrderTable';
import { WorkOrderFilters } from '../../components/workorders/WorkOrderFilters';
import { Pagination } from '../../components/common/Pagination';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { EmptyState } from '../../components/common/EmptyState';

export function ManagerWorkOrders() {
  const { data, loading, error, filters, updateFilters, resetFilters, refresh } =
    useWorkOrders();

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">All Work Orders</h1>
        <Link to="/manager/kanban" className="btn-secondary">
          Kanban View
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
          title="No work orders found"
          description="Try adjusting your filters."
        />
      ) : (
        <>
          <WorkOrderTable
            workOrders={data.content}
            basePath="/manager/work-orders"
          />
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
