import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { workOrdersApi } from '../../api/workOrders';
import { WorkOrderForm } from '../../components/workorders/WorkOrderForm';
import { PageSpinner } from '../../components/common/Spinner';
import { ErrorState } from '../../components/common/ErrorState';
import { useToast } from '../../components/common/Toast';
import type { WorkOrder, UpdateWorkOrderRequest } from '../../types';

export function EditWorkOrder() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    workOrdersApi.get(Number(id))
      .then(setWo)
      .catch(() => setError('Failed to load work order.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <PageSpinner />;
  if (error || !wo) return <ErrorState message={error ?? 'Not found.'} />;

  const handleSubmit = async (data: UpdateWorkOrderRequest) => {
    await workOrdersApi.update(wo.id, data);
    addToast('Work order updated', 'success');
    navigate(`/dispatcher/work-orders/${wo.id}`);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <nav className="text-sm text-gray-500">
        <Link to="/dispatcher/work-orders" className="hover:text-blue-600">Work Orders</Link>
        {' / '}
        <Link to={`/dispatcher/work-orders/${wo.id}`} className="hover:text-blue-600">#{wo.id}</Link>
        {' / '}
        <span className="text-gray-800">Edit</span>
      </nav>
      <h1 className="text-2xl font-bold text-gray-900">Edit Work Order</h1>
      <div className="card">
        <WorkOrderForm
          initial={wo}
          isEdit
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/dispatcher/work-orders/${wo.id}`)}
        />
      </div>
    </div>
  );
}
