import { priorityColors, priorityDot } from '../../utils/badges';
import type { Priority } from '../../types';

interface Props {
  priority: Priority;
}

export function PriorityBadge({ priority }: Props) {
  return (
    <span className={`badge gap-1.5 ${priorityColors[priority]}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${priorityDot[priority]}`} />
      {priority.charAt(0) + priority.slice(1).toLowerCase()}
    </span>
  );
}
