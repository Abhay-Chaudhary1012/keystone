import { Link } from 'react-router-dom';

export function ManagerTechnicians() {
  return (
    <div className="max-w-2xl mx-auto space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Technician Overview</h1>
        <p className="text-sm text-gray-500 mt-1">
          Technician directory and per-technician workload information
        </p>
      </div>

      <div className="card border border-blue-200 bg-blue-50">
        <div className="flex items-start gap-3">
          <div className="text-2xl" aria-hidden="true">ℹ️</div>
          <div>
            <h2 className="font-semibold text-blue-900">Technician directory is not available yet</h2>
            <p className="text-sm text-blue-700 mt-1">
              The current backend does not expose an endpoint for listing technicians.
              This page therefore does not use a fabricated /api/users request or display
              placeholder technician records.
            </p>
          </div>
        </div>
      </div>

      <div className="card">
        <h2 className="font-semibold text-gray-800 mb-3">Available operational data</h2>
        <p className="text-sm text-gray-500">
          Overall work-order status, parts cost, and technician time are available through
          the live reports summary endpoint.
        </p>
        <Link to="/manager/metrics" className="btn-primary inline-block mt-4">
          View Operational Metrics
        </Link>
      </div>

      <div className="card border border-dashed border-gray-200 bg-gray-50">
        <p className="text-sm font-medium text-gray-500">Future backend capability</p>
        <p className="text-xs text-gray-400 mt-1">
          A technician directory endpoint can be added later if per-technician workload,
          utilisation, SLA, or availability metrics are required.
        </p>
      </div>
    </div>
  );
}
