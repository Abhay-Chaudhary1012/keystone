import { statusColors, statusLabels } from '../../utils/badges';
import type { WorkOrderStatus } from '../../types';

interface Props {
  status: WorkOrderStatus;
}

export function StatusBadge({ status }: Props) {
  return (
    <span className={`badge ${statusColors[status]}`}>
      {statusLabels[status]}
    </span>
  );
}
