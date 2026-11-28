/** Lists are paged by the backend: page is zero-based, size 1..100, default 20 (SRS 3.4). */
export function Pagination({
  page,
  totalPages,
  totalItems,
  onPage,
}: {
  page: number;
  totalPages: number;
  totalItems: number;
  onPage: (page: number) => void;
}) {
  if (totalItems === 0) {
    return null;
  }
  return (
    <div className="pagination">
      <button type="button" className="secondary" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {Math.max(totalPages, 1)} ({totalItems} total)
      </span>
      <button type="button" className="secondary" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Next
      </button>
    </div>
  );
}
