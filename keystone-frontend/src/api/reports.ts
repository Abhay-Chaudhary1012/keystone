import apiClient from './client';
import type { ReportSummaryResponse } from '../types';

// Verified against ReportController: only GET /api/reports/summary exists.
// No @PreAuthorize on the backend — any authenticated role can call it.

export const reportsApi = {
  getSummary: () =>
    apiClient.get<ReportSummaryResponse>('/reports/summary').then((r) => r.data),
};
