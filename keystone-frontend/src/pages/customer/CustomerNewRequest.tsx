import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { workOrdersApi } from '../../api/workOrders';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../components/common/Toast';
import { Spinner } from '../../components/common/Spinner';

const schema = z.object({
  title: z.string().min(5, 'Please provide a brief title (min 5 chars)'),
  description: z
    .string()
    .min(20, 'Please describe the issue in detail (min 20 chars)'),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']),
});

type FormValues = z.infer<typeof schema>;

export function CustomerNewRequest() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { priority: 'MEDIUM' },
  });

  const onSubmit = async (values: FormValues) => {
    if (!user?.customerId) {
      addToast('Your account is not linked to a customer profile.', 'error');
      return;
    }
    try {
      const wo = await workOrdersApi.create({
        title: values.title,
        description: values.description,
        priority: values.priority,
        customerId: user.customerId,
      });
      addToast('Service request submitted successfully!', 'success');
      navigate(`/customer/requests/${wo.id}`);
    } catch {
      addToast('Failed to submit request. Please try again.', 'error');
    }
  };

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">New Service Request</h1>
        <p className="text-sm text-gray-500 mt-1">
          Describe the issue and our team will be in touch.
        </p>
      </div>

      <div className="card">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
          <div>
            <label className="label" htmlFor="sr-title">
              Subject *
            </label>
            <input
              id="sr-title"
              className={`input ${errors.title ? 'border-red-400' : ''}`}
              placeholder="e.g. HVAC not cooling in meeting room 3"
              {...register('title')}
            />
            {errors.title && (
              <p className="mt-1 text-xs text-red-500">{errors.title.message}</p>
            )}
          </div>

          <div>
            <label className="label" htmlFor="sr-desc">
              Description *
            </label>
            <textarea
              id="sr-desc"
              rows={5}
              className={`input resize-none ${errors.description ? 'border-red-400' : ''}`}
              placeholder="Describe the issue in as much detail as possible — what's happening, when it started, any relevant context."
              {...register('description')}
            />
            {errors.description && (
              <p className="mt-1 text-xs text-red-500">
                {errors.description.message}
              </p>
            )}
          </div>

          <div>
            <label className="label" htmlFor="sr-priority">
              Priority
            </label>
            <select
              id="sr-priority"
              className="input"
              {...register('priority')}
            >
              <option value="LOW">Low — not urgent</option>
              <option value="MEDIUM">Medium — needs attention</option>
              <option value="HIGH">High — affecting operations</option>
              <option value="CRITICAL">Critical — immediate response needed</option>
            </select>
          </div>

          <div className="pt-2 flex gap-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary"
            >
              {isSubmitting ? (
                <span className="flex items-center gap-2">
                  <Spinner size="sm" className="text-white" /> Submitting…
                </span>
              ) : (
                'Submit Request'
              )}
            </button>
            <button
              type="button"
              onClick={() => navigate('/customer')}
              className="btn-secondary"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>

      <div className="card bg-blue-50 border border-blue-200">
        <p className="text-sm text-blue-700">
          <strong>What happens next?</strong> A dispatcher will review your
          request and assign a technician. You can track the status on the{' '}
          <span className="underline cursor-pointer" onClick={() => navigate('/customer/requests')}>
            My Requests
          </span>{' '}
          page.
        </p>
      </div>
    </div>
  );
}
