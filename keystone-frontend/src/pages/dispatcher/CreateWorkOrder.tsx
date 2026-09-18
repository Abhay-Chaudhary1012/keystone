import { useNavigate } from 'react-router-dom';
import { WorkOrderForm } from '../../components/workorders/WorkOrderForm';
import { workOrdersApi } from '../../api/workOrders';
import { useToast } from '../../components/common/Toast';
import type { CreateWorkOrderRequest, UpdateWorkOrderRequest } from '../../types';

export function CreateWorkOrder() {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const handleSubmit = async (data: CreateWorkOrderRequest | UpdateWorkOrderRequest) => {
    // On the create page this will always be a CreateWorkOrderRequest
    const wo = await workOrdersApi.create(data as CreateWorkOrderRequest);
    addToast('Work order created successfully!', 'success');
    navigate(`/dispatcher/work-orders/${wo.id}`);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">New Work Order</h1>
        <p className="text-sm text-gray-500 mt-1">
          Create a new field service work order
        </p>
      </div>
      <div className="card">
        <WorkOrderForm
          onSubmit={handleSubmit}
          onCancel={() => navigate('/dispatcher/work-orders')}
        />
      </div>
    </div>
  );
}
