import type { WorkOrderFilterParams, WorkOrderStatus, Priority } from '../../types';

const STATUSES: WorkOrderStatus[] = ['OPEN','ASSIGNED','IN_PROGRESS','ON_HOLD','COMPLETED','CANCELLED'];
const PRIORITIES: Priority[] = ['LOW','MEDIUM','HIGH','CRITICAL'];
const STATUS_LABELS: Record<WorkOrderStatus, string> = {
  OPEN:'Open', ASSIGNED:'Assigned', IN_PROGRESS:'In Progress',
  ON_HOLD:'On Hold', COMPLETED:'Completed', CANCELLED:'Cancelled',
};

interface Props {
  filters: WorkOrderFilterParams;
  onChange: (f: Partial<WorkOrderFilterParams>) => void;
  onReset: () => void;
}

export function WorkOrderFilters({ filters, onChange, onReset }: Props) {
  return (
    <div className="flex flex-col sm:flex-row gap-3">
      <input
        type="search"
        placeholder="Search work orders…"
        value={filters.search ?? ''}
        onChange={(e) => onChange({ search: e.target.value, page: 0 })}
        className="input sm:w-64"
      />
      <select
        value={filters.status ?? ''}
        onChange={(e) => onChange({ status: (e.target.value as WorkOrderStatus) || undefined, page: 0 })}
        className="input sm:w-44"
      >
        <option value="">All Statuses</option>
        {STATUSES.map((s) => (
          <option key={s} value={s}>{STATUS_LABELS[s]}</option>
        ))}
      </select>
      <select
        value={filters.priority ?? ''}
        onChange={(e) => onChange({ priority: (e.target.value as Priority) || undefined, page: 0 })}
        className="input sm:w-40"
      >
        <option value="">All Priorities</option>
        {PRIORITIES.map((p) => (
          <option key={p} value={p}>{p.charAt(0) + p.slice(1).toLowerCase()}</option>
        ))}
      </select>
      {(filters.search || filters.status || filters.priority) && (
        <button onClick={onReset} className="btn-secondary text-xs">
          Clear filters
        </button>
      )}
    </div>
  );
}
