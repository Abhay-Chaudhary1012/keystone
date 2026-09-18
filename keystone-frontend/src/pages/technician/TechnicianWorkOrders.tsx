import { useWorkOrders } from '../../hooks/useWorkOrders';
import { WorkOrderTable } from '../../components/workorders/WorkOrderTable';
import { WorkOrderFilters } from '../../components/workorders/WorkOrderFilters';
import { Pagination } from '../../components/common/Pagination';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { EmptyState } from '../../components/common/EmptyState';

export function TechnicianWorkOrders() {
  const { data, loading, error, filters, updateFilters, resetFilters, refresh } = useWorkOrders();

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">My Work Orders</h1>

      <WorkOrderFilters filters={filters} onChange={updateFilters} onReset={resetFilters} />

      {loading ? (
        <PageSpinner />
      ) : error ? (
        <ErrorState message={error} onRetry={refresh} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="No work orders assigned to you"
          description="Work orders assigned to you will appear here."
        />
      ) : (
        <>
          <WorkOrderTable workOrders={data.content} basePath="/technician/work-orders" />
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
