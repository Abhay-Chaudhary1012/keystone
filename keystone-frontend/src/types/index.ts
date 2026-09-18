// ─── Enums ────────────────────────────────────────────────────────────────────
// Verified against backend enums (com.keystone.keystone_backend.enums.*).

export type Role = 'DISPATCHER' | 'TECHNICIAN' | 'MANAGER' | 'CUSTOMER';

export type WorkOrderStatus =
  | 'OPEN'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

// ─── Auth ─────────────────────────────────────────────────────────────────────
// Verified against AuthController / AuthResponse / LoginRequest / UserPrincipal.

export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Shape returned by both POST /api/auth/login and GET /api/auth/me.
 * This is a FLAT object — there is no nested `user` field. `token` is present
 * only on the login response (omitted via @JsonInclude(NON_NULL) on /me).
 * `customerId` is present only for CUSTOMER-role users (null/absent for
 * internal staff) — this is how the backend identifies which Customer
 * organization a CUSTOMER-role user belongs to.
 */
export interface AuthResponse {
  token?: string;
  tokenType?: string;
  userId: number;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  customerId?: number;
}

/** The authenticated user, as stored in AuthContext (token stripped out). */
export type AuthUser = Omit<AuthResponse, 'token' | 'tokenType'>;

// ─── Work Orders ────────────────────────────────────────────────────────────
// Verified against WorkOrderController / WorkOrderService / WorkOrderResponse.
//
// IMPORTANT: WorkOrderResponse is FLAT and DENORMALIZED. There are no nested
// `customer` / `site` / `assignedTechnician` / `createdBy` objects — only
// `<x>Id` and `<x>Name` string/number pairs the backend already resolved.
// There is also no `scheduledDate` or `completedDate` field anywhere in the
// backend (CreateWorkOrderRequest, UpdateWorkOrderRequest, WorkOrderResponse) —
// those fields do not exist in this API and must not be sent or displayed.

export interface WorkOrder {
  id: number;
  code: string;
  title: string;
  description: string;
  status: WorkOrderStatus;
  priority: Priority;
  notes?: string;

  customerId: number;
  customerName: string;
  customerCode: string;

  siteId?: number;
  siteName?: string;

  assignedTechnicianId?: number;
  assignedTechnicianName?: string;

  createdById: number;
  createdByName: string;

  createdAt: string;
  updatedAt: string;
}

export interface CreateWorkOrderRequest {
  title: string;
  description?: string;
  priority: Priority;
  customerId: number;
  siteId?: number;
  notes?: string;
}

export interface UpdateWorkOrderRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  siteId?: number;
  notes?: string;
}

/** POST /api/work-orders/{id}/assign */
export interface AssignTechnicianRequest {
  technicianId: number;
}

/**
 * Query params accepted by GET /api/work-orders. The backend does NOT accept
 * customerId/technicianId filters — CUSTOMER and TECHNICIAN principals are
 * auto-scoped server-side (to their own org / their own assigned WOs
 * respectively) based on the JWT, not a query param.
 */
export interface WorkOrderFilterParams {
  status?: WorkOrderStatus;
  priority?: Priority;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ─── Parts / Time (WorkOrderExecutionController) ───────────────────────────
// Verified against LogPartRequest / LogTimeRequest / WorkOrderPartResponse /
// TimeEntryResponse / PartService / TimeEntryService.
//
// Both endpoints are POST-only, TECHNICIAN-only, and require the technician
// to be the work order's assigned technician. There is NO GET or DELETE
// endpoint for parts or time entries anywhere in the backend — logged
// entries cannot be listed or removed via this API.

export interface LogPartRequest {
  partId: number;
  quantity: number;
}

export interface LogTimeRequest {
  minutes: number;
  notes?: string;
}

export interface WorkOrderPartResponse {
  id: number;
  workOrderId: number;
  workOrderCode: string;
  partId: number;
  partName: string;
  partNumber: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  createdAt: string;
}

export interface TimeEntryResponse {
  id: number;
  workOrderId: number;
  workOrderCode: string;
  technicianId: number;
  technicianName: string;
  minutes: number;
  notes?: string;
  createdAt: string;
}

// ─── Reports (ReportController) ────────────────────────────────────────────
// Verified against ReportController / ReportService / ReportSummaryResponse.
// GET /api/reports/summary has no @PreAuthorize — any authenticated role can
// call it — but it is only wired into the Manager Metrics page here, matching
// the app's existing structure.

export interface ReportSummaryResponse {
  totalWorkOrders: number;
  openWorkOrders: number;
  assignedWorkOrders: number;
  inProgressWorkOrders: number;
  onHoldWorkOrders: number;
  completedWorkOrders: number;
  cancelledWorkOrders: number;
  totalPartsCost: number;
  totalTechnicianMinutes: number;
}

// ─── API Shapes ───────────────────────────────────────────────────────────────

/** Spring Data's default Page<T> JSON — only the fields this app uses. */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-indexed)
  first: boolean;
  last: boolean;
}

/** Verified against ApiErrorResponse / GlobalExceptionHandler. */
export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp?: string;
  path?: string;
  /** Only present for 400 Bean Validation failures, e.g. ["title: must not be blank"] */
  validationErrors?: string[];
}

// ─── UI State ─────────────────────────────────────────────────────────────────

export interface SelectOption {
  value: string | number;
  label: string;
}
