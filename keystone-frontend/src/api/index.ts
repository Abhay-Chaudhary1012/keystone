export { default as apiClient } from './client';
export { authApi } from './auth';
export { workOrdersApi } from './workOrders';
export { reportsApi } from './reports';

// NOTE: there is no UserController or CustomerController on the backend —
// no /api/users or /api/customers endpoints exist at all. The previous
// api/users.ts and api/customers.ts files called endpoints that don't exist
// and have been removed. See README_CHECKPOINT.md for details.
