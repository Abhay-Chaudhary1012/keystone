# KEYSTONE Frontend

React + TypeScript + Vite + Tailwind CSS frontend for the KEYSTONE Field Service Management platform.

## Quick Start

```bash
# 1. Install dependencies
npm install

# 2. Configure environment
cp .env.example .env.local
# Edit .env.local — set VITE_API_BASE_URL to your backend URL

# 3. Start development server
npm run dev

# 4. Production build
npm run build
```

## Environment

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend base URL (no trailing slash) |

## Project Structure

```
src/
├── api/              # Axios API layer (one file per resource)
│   ├── client.ts     # Instance + JWT interceptor + 401 redirect
│   ├── auth.ts       # login / me / logout
│   ├── workOrders.ts # CRUD + all lifecycle transitions
├── components/
│   ├── common/       # StatusBadge, PriorityBadge, Spinner, Pagination,
│   │                 #   Toast, EmptyState, ErrorState, ConfirmDialog
│   ├── kanban/       # KanbanBoard (6 status columns)
│   ├── layout/       # Sidebar (role-filtered nav) + Header (user menu)
│   └── workorders/   # WorkOrderTable, WorkOrderFilters, WorkOrderForm
├── context/
│   └── AuthContext.tsx   # JWT storage, login/logout, hasRole()
├── hooks/
│   └── useWorkOrders.ts  # Paginated + filtered work order hook
├── layouts/
│   └── AppLayout.tsx     # Responsive shell
├── pages/
│   ├── auth/         # Login, Profile, Unauthorized (403), NotFound (404)
│   ├── dispatcher/   # Dashboard, WO list, WO detail, Create, Edit, Kanban
│   ├── technician/   # Dashboard, My WOs, WO detail (lifecycle actions)
│   ├── manager/      # Dashboard, All WOs, WO detail, Technicians, Metrics, Kanban
│   └── customer/     # Dashboard, My Requests, New Request, Request detail
├── routes/
│   ├── ProtectedRoute.tsx   # Auth + role guard (→ /login or /unauthorized)
│   └── RoleRedirect.tsx     # Post-login role-home redirect
├── types/index.ts    # All TypeScript interfaces
└── utils/
    ├── badges.ts     # Status/priority colour maps
    └── format.ts     # formatDate, formatDateTime, fullName, initials
```

## Roles & Routes

| Role | Home | Key Pages |
|---|---|---|
| `DISPATCHER` | `/dispatcher` | Dashboard, Work Orders (list/detail/create/edit), Kanban, Assign Technician |
| `TECHNICIAN` | `/technician` | Dashboard, My Work Orders, WO detail (Start / Hold / Resume / Complete) |
| `MANAGER`    | `/manager`    | Dashboard, All WOs, Kanban, Technician Overview, Metrics |
| `CUSTOMER`   | `/customer`   | Dashboard, My Requests (list/detail), New Request, Status tracker |

## Authentication Flow

1. User POSTs to `POST /api/auth/login` → receives JWT + user object
2. JWT stored in `localStorage` (`ks_token`); user object in `ks_user`
3. On every app load `GET /api/auth/me` verifies the token is still valid
4. Every Axios request automatically includes `Authorization: Bearer <token>`
5. Any 401 response clears the token and redirects to `/login`
6. Role guards: wrong role → `/unauthorized` (403 page)

## Work Order Lifecycle

```
OPEN → (assign) → ASSIGNED → (start) → IN_PROGRESS → (complete) → COMPLETED
                                ↕ hold/resume
                            ON_HOLD

Any non-terminal status → (cancel) → CANCELLED
```

All transitions call the backend API. Zero client-side status mutations.

## Backend Integration Status

### ✅ Integrated (existing backend endpoints)
| Method | Path |
|---|---|
| POST | `/api/auth/login` |
| GET  | `/api/auth/me` |
| GET  | `/api/work-orders` (filter, search, pagination) |
| POST | `/api/work-orders` |
| GET  | `/api/work-orders/{id}` |
| PUT  | `/api/work-orders/{id}` |
| DELETE | `/api/work-orders/{id}` |
| POST | `/api/work-orders/{id}/start` |
| POST | `/api/work-orders/{id}/hold` |
| POST | `/api/work-orders/{id}/resume` |
| POST | `/api/work-orders/{id}/complete` |
| POST | `/api/work-orders/{id}/cancel` |

### ⏳ Awaiting Backend (UI placeholders in place)
| Frontend call | Backend endpoint needed | UI behaviour now |
|---|---|---|
| `workOrdersApi.assign()` | `POST /api/work-orders/{id}/assign` | Assign button wired; fails gracefully |
| `usersApi.getTechnicians()` | `GET /api/users?role=TECHNICIAN` | Assign UI shows ⚠ if 0 results |
| `customersApi.list()` | `GET /api/customers` | Form shows ⚠ if unavailable |
| `customersApi.getSites()` | `GET /api/customers/{id}/sites` | Site selector disabled if no results |
| `authApi.logout()` | `POST /api/auth/logout` | Silent fail; token still cleared locally |
| Avg. resolution time | `GET /api/metrics/resolution-time` | Placeholder card |
| SLA compliance | `GET /api/metrics/sla` | Placeholder card |
| Time entries | `POST /api/work-orders/{id}/time-entries` | Placeholder section |
| Parts tracking | `POST /api/work-orders/{id}/parts` | Placeholder section |

## Tech Stack

- React 18 · TypeScript (strict) · Vite 8
- Tailwind CSS 3 · React Router DOM 6
- React Hook Form + Zod · Axios
