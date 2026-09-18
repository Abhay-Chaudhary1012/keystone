import apiClient from './client';
import type {
  WorkOrder,
  PageResponse,
  CreateWorkOrderRequest,
  UpdateWorkOrderRequest,
  AssignTechnicianRequest,
  WorkOrderFilterParams,
  LogPartRequest,
  LogTimeRequest,
  WorkOrderPartResponse,
  TimeEntryResponse,
} from '../types';

const BASE = '/work-orders';

// Verified against WorkOrderController / WorkOrderExecutionController.
//
// - There is NO DELETE /api/work-orders/{id} endpoint on the backend at all.
//   "Deleting" a work order isn't supported — cancel() is the only removal-
//   like operation, and it's a status change (POST /cancel), not a delete.
// - start/hold/resume/complete/cancel take NO request body — the controller
//   methods don't declare a @RequestBody param, so there's nowhere on the
//   backend to send a "reason" or "completion notes" string. Those aren't
//   supported by this API.
// - logPart/logTime return WorkOrderPartResponse / TimeEntryResponse, NOT
//   the WorkOrder — logging a part or time entry does not change the work
//   order's status or fields.

export const workOrdersApi = {
  // ── CRUD ──────────────────────────────────────────────────────────────────

  list: (params: WorkOrderFilterParams = {}) =>
    apiClient
      .get<PageResponse<WorkOrder>>(BASE, { params })
      .then((r) => r.data),

  get: (id: number) =>
    apiClient.get<WorkOrder>(`${BASE}/${id}`).then((r) => r.data),

  create: (data: CreateWorkOrderRequest) =>
    apiClient.post<WorkOrder>(BASE, data).then((r) => r.data),

  update: (id: number, data: UpdateWorkOrderRequest) =>
    apiClient.put<WorkOrder>(`${BASE}/${id}`, data).then((r) => r.data),

  // ── Lifecycle transitions ─────────────────────────────────────────────────

  assign: (id: number, data: AssignTechnicianRequest) =>
    apiClient.post<WorkOrder>(`${BASE}/${id}/assign`, data).then((r) => r.data),

  start: (id: number) =>
    apiClient.post<WorkOrder>(`${BASE}/${id}/start`).then((r) => r.data),

  hold: (id: number) =>
    apiClient.post<WorkOrder>(`${BASE}/${id}/hold`).then((r) => r.data),

  resume: (id: number) =>
    apiClient.post<WorkOrder>(`${BASE}/${id}/resume`).then((r) => r.data),

  complete: (id: number) =>
    apiClient.post<WorkOrder>(`${BASE}/${id}/complete`).then((r) => r.data),

  cancel: (id: number) =>
    apiClient.post<WorkOrder>(`${BASE}/${id}/cancel`).then((r) => r.data),

  // ── Parts / Time (WorkOrderExecutionController) ─────────────────────────

  logPart: (id: number, data: LogPartRequest) =>
    apiClient.post<WorkOrderPartResponse>(`${BASE}/${id}/parts`, data).then((r) => r.data),

  logTime: (id: number, data: LogTimeRequest) =>
    apiClient.post<TimeEntryResponse>(`${BASE}/${id}/time`, data).then((r) => r.data),
};
