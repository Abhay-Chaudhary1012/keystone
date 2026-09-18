interface Props {
  page: number;        // 0-indexed
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
}

/** Returns the page numbers (0-indexed) to display, with -1 used as an ellipsis sentinel. */
function pageWindow(current: number, total: number): number[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages: number[] = [0];
  const lo = Math.max(1, current - 1);
  const hi = Math.min(total - 2, current + 1);
  if (lo > 1) pages.push(-1);
  for (let i = lo; i <= hi; i++) pages.push(i);
  if (hi < total - 2) pages.push(-1);
  pages.push(total - 1);
  return pages;
}

export function Pagination({ page, totalPages, totalElements, size, onPageChange }: Props) {
  if (totalPages <= 1) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  const pages = pageWindow(page, totalPages);

  return (
    <div className="flex items-center justify-between px-1 py-3 text-sm text-gray-600 flex-wrap gap-2">
      <span>
        Showing <strong>{from}–{to}</strong> of <strong>{totalElements}</strong>
      </span>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="px-2 py-1 rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          ‹
        </button>
        {pages.map((p, idx) =>
          p === -1 ? (
            <span key={`ellipsis-${idx}`} className="px-2 py-1 text-gray-400">…</span>
          ) : (
            <button
              key={p}
              onClick={() => onPageChange(p)}
              className={`px-3 py-1 rounded border text-sm ${
                p === page
                  ? 'bg-blue-600 text-white border-blue-600'
                  : 'border-gray-200 hover:bg-gray-50'
              }`}
            >
              {p + 1}
            </button>
          ),
        )}
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="px-2 py-1 rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          ›
        </button>
      </div>
    </div>
  );
}
