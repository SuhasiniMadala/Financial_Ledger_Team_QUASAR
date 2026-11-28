/** yyyy-MM-dd dates, inclusive, UTC (backend DateRange). Native date inputs always produce that format. */
export function DateRangeFilter({
  from,
  to,
  onChange,
}: {
  from: string;
  to: string;
  onChange: (range: { from: string; to: string }) => void;
}) {
  return (
    <>
      <label>
        From
        <input type="date" value={from} onChange={(e) => onChange({ from: e.target.value, to })} />
      </label>
      <label>
        To
        <input type="date" value={to} onChange={(e) => onChange({ from, to: e.target.value })} />
      </label>
    </>
  );
}
