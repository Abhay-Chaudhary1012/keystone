import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './components/common/Toast';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { RoleRedirect } from './routes/RoleRedirect';
import { AppLayout } from './layouts/AppLayout';

// Auth pages
import { LoginPage } from './pages/auth/LoginPage';
import { ProfilePage } from './pages/auth/ProfilePage';
import { UnauthorizedPage } from './pages/auth/UnauthorizedPage';
import { NotFoundPage } from './pages/auth/NotFoundPage';

// Dispatcher pages
import { DispatcherDashboard } from './pages/dispatcher/DispatcherDashboard';
import { DispatcherWorkOrders } from './pages/dispatcher/DispatcherWorkOrders';
import { DispatcherWorkOrderDetail } from './pages/dispatcher/DispatcherWorkOrderDetail';
import { CreateWorkOrder } from './pages/dispatcher/CreateWorkOrder';
import { EditWorkOrder } from './pages/dispatcher/EditWorkOrder';
import { DispatcherKanban } from './pages/dispatcher/DispatcherKanban';

// Technician pages
import { TechnicianDashboard } from './pages/technician/TechnicianDashboard';
import { TechnicianWorkOrders } from './pages/technician/TechnicianWorkOrders';
import { TechnicianWorkOrderDetail } from './pages/technician/TechnicianWorkOrderDetail';

// Manager pages
import { ManagerDashboard } from './pages/manager/ManagerDashboard';
import { ManagerWorkOrders } from './pages/manager/ManagerWorkOrders';
import { ManagerWorkOrderDetail } from './pages/manager/ManagerWorkOrderDetail';
import { ManagerTechnicians } from './pages/manager/ManagerTechnicians';
import { ManagerMetrics } from './pages/manager/ManagerMetrics';
import { ManagerKanban } from './pages/manager/ManagerKanban';

// Customer pages
import { CustomerDashboard } from './pages/customer/CustomerDashboard';
import { CustomerRequests } from './pages/customer/CustomerRequests';
import { CustomerNewRequest } from './pages/customer/CustomerNewRequest';
import { CustomerRequestDetail } from './pages/customer/CustomerRequestDetail';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            {/* ── Public ──────────────────────────────────────────────── */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/unauthorized" element={<UnauthorizedPage />} />

            {/* ── Root redirect ────────────────────────────────────────── */}
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<RoleRedirect />} />
            </Route>

            {/* ── Dispatcher ──────────────────────────────────────────── */}
            <Route element={<ProtectedRoute allowedRoles={['DISPATCHER']} />}>
              <Route element={<AppLayout />}>
                <Route path="/dispatcher" element={<DispatcherDashboard />} />
                <Route path="/dispatcher/work-orders" element={<DispatcherWorkOrders />} />
                <Route path="/dispatcher/work-orders/new" element={<CreateWorkOrder />} />
                <Route path="/dispatcher/work-orders/:id" element={<DispatcherWorkOrderDetail />} />
                <Route path="/dispatcher/work-orders/:id/edit" element={<EditWorkOrder />} />
                <Route path="/dispatcher/kanban" element={<DispatcherKanban />} />
                <Route path="/dispatcher/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* ── Technician ──────────────────────────────────────────── */}
            <Route element={<ProtectedRoute allowedRoles={['TECHNICIAN']} />}>
              <Route element={<AppLayout />}>
                <Route path="/technician" element={<TechnicianDashboard />} />
                <Route path="/technician/work-orders" element={<TechnicianWorkOrders />} />
                <Route path="/technician/work-orders/:id" element={<TechnicianWorkOrderDetail />} />
                <Route path="/technician/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* ── Manager ─────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute allowedRoles={['MANAGER']} />}>
              <Route element={<AppLayout />}>
                <Route path="/manager" element={<ManagerDashboard />} />
                <Route path="/manager/work-orders" element={<ManagerWorkOrders />} />
                <Route path="/manager/work-orders/:id" element={<ManagerWorkOrderDetail />} />
                <Route path="/manager/technicians" element={<ManagerTechnicians />} />
                <Route path="/manager/metrics" element={<ManagerMetrics />} />
                <Route path="/manager/kanban" element={<ManagerKanban />} />
                <Route path="/manager/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* ── Customer ─────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute allowedRoles={['CUSTOMER']} />}>
              <Route element={<AppLayout />}>
                <Route path="/customer" element={<CustomerDashboard />} />
                <Route path="/customer/requests" element={<CustomerRequests />} />
                <Route path="/customer/requests/new" element={<CustomerNewRequest />} />
                <Route path="/customer/requests/:id" element={<CustomerRequestDetail />} />
                <Route path="/customer/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* ── 404 ─────────────────────────────────────────────────── */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
