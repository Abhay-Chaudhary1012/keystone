import type { WorkOrderStatus, Priority } from '../types';

export const statusColors: Record<WorkOrderStatus, string> = {
  OPEN:        'bg-blue-100 text-blue-800',
  ASSIGNED:    'bg-purple-100 text-purple-800',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
  ON_HOLD:     'bg-orange-100 text-orange-800',
  COMPLETED:   'bg-green-100 text-green-800',
  CANCELLED:   'bg-gray-100 text-gray-600',
};

export const statusLabels: Record<WorkOrderStatus, string> = {
  OPEN:        'Open',
  ASSIGNED:    'Assigned',
  IN_PROGRESS: 'In Progress',
  ON_HOLD:     'On Hold',
  COMPLETED:   'Completed',
  CANCELLED:   'Cancelled',
};

export const priorityColors: Record<Priority, string> = {
  LOW:      'bg-gray-100 text-gray-600',
  MEDIUM:   'bg-blue-100 text-blue-700',
  HIGH:     'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-red-100 text-red-700',
};

export const priorityDot: Record<Priority, string> = {
  LOW:      'bg-gray-400',
  MEDIUM:   'bg-blue-500',
  HIGH:     'bg-orange-500',
  CRITICAL: 'bg-red-600',
};
