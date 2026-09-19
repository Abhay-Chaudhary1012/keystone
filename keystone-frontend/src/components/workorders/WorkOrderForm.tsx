import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type {
  WorkOrder,
  CreateWorkOrderRequest,
  UpdateWorkOrderRequest,
  Priority,
} from '../../types';
import { Spinner } from '../common/Spinner';

const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

// NOTE: there is no CustomerController/UserController on the backend — no
// GET /api/customers or GET /api/customers/{id}/sites endpoint exists to
// power a picker. Customer/Site are plain numeric ID fields until such an
// endpoint exists. There is also no `scheduledDate` field anywhere in the
// backend's CreateWorkOrderRequest/UpdateWorkOrderRequest — it has been
// removed rather than silently dropped by the server.
const schema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']),
  customerId: z.number().min(1, 'Customer ID is required'),
  siteId: z.number().optional(),
  notes: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  initial?: WorkOrder;
  onSubmit: (data: CreateWorkOrderRequest | UpdateWorkOrderRequest) => Promise<void>;
  onCancel: () => void;
  isEdit?: boolean;
}

export function WorkOrderForm({ initial, onSubmit, onCancel, isEdit }: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: initial?.title ?? '',
      description: initial?.description ?? '',
      priority: initial?.priority ?? 'MEDIUM',
      customerId: initial?.customerId,
      siteId: initial?.siteId,
      notes: initial?.notes ?? '',
    },
  });

  const handleFormSubmit = async (values: FormValues) => {
    const payload: CreateWorkOrderRequest | UpdateWorkOrderRequest = isEdit
      ? {
          title: values.title,
          description: values.description,
          priority: values.priority,
          siteId: values.siteId || undefined,
          notes: values.notes || undefined,
        }
      : {
          title: values.title,
          description: values.description,
          priority: values.priority,
          customerId: values.customerId,
          siteId: values.siteId || undefined,
          notes: values.notes || undefined,
        };
    await onSubmit(payload);
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} noValidate className="space-y-5">
      <div>
        <label className="label" htmlFor="wo-title">
          Title *
        </label>
        <input
          id="wo-title"
          className={`input ${errors.title ? 'border-red-400' : ''}`}
          placeholder="Brief description of the work"
          {...register('title')}
        />
        {errors.title && (
          <p className="mt-1 text-xs text-red-500">{errors.title.message}</p>
        )}
      </div>

      <div>
        <label className="label" htmlFor="wo-desc">
          Description *
        </label>
        <textarea
          id="wo-desc"
          rows={4}
          className={`input resize-none ${errors.description ? 'border-red-400' : ''}`}
          placeholder="Detailed description of the issue or work to be performed"
          {...register('description')}
        />
        {errors.description && (
          <p className="mt-1 text-xs text-red-500">{errors.description.message}</p>
        )}
      </div>

      <div>
        <label className="label" htmlFor="wo-priority">
          Priority *
        </label>
        <select id="wo-priority" className="input" {...register('priority')}>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p.charAt(0) + p.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="label" htmlFor="wo-customer">
            Customer ID *
          </label>
          <input
            id="wo-customer"
            type="number"
            min={1}
            disabled={isEdit}
            className={`input ${errors.customerId ? 'border-red-400' : ''}`}
            placeholder="e.g. 3"
            {...register('customerId', { valueAsNumber: true })}
          />
          {errors.customerId && (
            <p className="mt-1 text-xs text-red-500">{errors.customerId.message}</p>
          )}
          {!isEdit && (
            <p className="mt-1 text-xs text-gray-400">
              No customer directory endpoint exists yet — enter the numeric ID directly.
            </p>
          )}
        </div>

        <div>
          <label className="label" htmlFor="wo-site">
            Site ID
          </label>
          <input
            id="wo-site"
            type="number"
            min={1}
            className="input"
            placeholder="Optional"
            {...register('siteId', { setValueAs: (value) => value === '' ? undefined : Number(value) })}
          />
          <p className="mt-1 text-xs text-gray-400">
            Must belong to the customer above — the backend validates this.
          </p>
        </div>
      </div>

      <div>
        <label className="label" htmlFor="wo-notes">
          Notes
        </label>
        <textarea
          id="wo-notes"
          rows={3}
          className="input resize-none"
          placeholder="Additional notes or special instructions"
          {...register('notes')}
        />
      </div>

      <div className="flex gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={isSubmitting}>
          {isSubmitting ? <Spinner size="sm" className="text-white" /> : null}
          {isEdit ? 'Update Work Order' : 'Create Work Order'}
        </button>
        <button type="button" onClick={onCancel} className="btn-secondary">
          Cancel
        </button>
      </div>
    </form>
  );
}

