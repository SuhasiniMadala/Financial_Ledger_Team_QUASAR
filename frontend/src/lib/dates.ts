/** Backend timestamps are ISO-8601 UTC (SRS 3.4). Shown as "2026-11-28 10:15:02 UTC" without time-zone guessing. */
export function formatTimestamp(iso: string | undefined): string {
  if (!iso) {
    return '';
  }
  const match = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})/.exec(iso);
  return match ? `${match[1]} ${match[2]} UTC` : iso;
}

/** Date filters are yyyy-MM-dd, inclusive, UTC (backend DateRange). */
export const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
