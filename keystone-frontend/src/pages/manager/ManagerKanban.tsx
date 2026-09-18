import { useEffect, useState } from 'react';
import { workOrdersApi } from '../../api/workOrders';
import { KanbanBoard } from '../../components/kanban/KanbanBoard';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import type { WorkOrder } from '../../types';

export function ManagerKanban() {
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    workOrdersApi
      .list({ size: 200 })
      .then((r) => setWorkOrders(r.content))
      .catch(() => setError('Failed to load kanban data.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Kanban Board</h1>
        <p className="text-sm text-gray-500 mt-1">
          Visual overview of all work orders by status
        </p>
      </div>
      {loading ? (
        <PageSpinner />
      ) : error ? (
        <ErrorState message={error} />
      ) : (
        <KanbanBoard workOrders={workOrders} basePath="/manager/work-orders" />
      )}
    </div>
  );
}
