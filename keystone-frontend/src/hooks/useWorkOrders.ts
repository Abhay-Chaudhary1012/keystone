import { useState, useEffect, useCallback } from 'react';
import { workOrdersApi } from '../api/workOrders';
import type { WorkOrder, PageResponse, WorkOrderFilterParams } from '../types';

const DEFAULT_FILTERS: WorkOrderFilterParams = { page: 0, size: 20 };

export function useWorkOrders(initialFilters?: WorkOrderFilterParams) {
  const [filters, setFilters] = useState<WorkOrderFilterParams>({ ...DEFAULT_FILTERS, ...initialFilters });
  const [data, setData] = useState<PageResponse<WorkOrder> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async (f: WorkOrderFilterParams) => {
    setLoading(true);
    setError(null);
    try {
      const allowedParams: WorkOrderFilterParams = {
        status: f.status,
        priority: f.priority,
        search: f.search,
        page: f.page,
        size: f.size,
        sort: f.sort,
      };
      const res = await workOrdersApi.list(allowedParams);
      setData(res);
    } catch {
      setError('Failed to load work orders. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetch(filters); }, [fetch, filters]);

  const updateFilters = useCallback((partial: Partial<WorkOrderFilterParams>) => {
    setFilters((prev) => ({ ...prev, ...partial }));
  }, []);

  const resetFilters = useCallback(() => {
    setFilters({ ...DEFAULT_FILTERS, ...initialFilters });
  }, [initialFilters]);

  const refresh = useCallback(() => fetch(filters), [fetch, filters]);

  return { data, loading, error, filters, updateFilters, resetFilters, refresh };
}
