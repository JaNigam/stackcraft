/**
 * A row of bars for "walls and water" problems. Bars are sticker blocks; water is drawn either per column on top of
 * each bar (Trapping Rain Water) or as one rectangle between two walls at a level (Container With Most Water).
 * Heights are drawn to scale against the tallest bar, so the picture matches the numbers.
 */
export interface BarsProps {
  heights: number[];
  /** Tag a column as the left or right pointer. */
  left?: number;
  right?: number;
  /** Per-column water units sitting on top of each bar. */
  water?: number[];
  /** One container of water between two walls (exclusive of the walls), at this level. */
  container?: { from: number; to: number; level: number };
  /** Draw bars as thin walls (Container) instead of solid blocks (Rain Water). */
  thin?: boolean;
  /** Columns already settled (dimmed). */
  settled?: (i: number) => boolean;
  label: string;
}

export function Bars({ heights, left, right, water, container, thin, settled, label }: BarsProps) {
  const n = heights.length;
  const top = Math.max(1, ...heights, ...(container ? [container.level] : []));
  const pct = (v: number) => `${(v / top) * 100}%`;
  return (
    <div className={`wb${thin ? ' wb--thin' : ''}`} role="img" aria-label={label}>
      <div className="wb__plot" style={{ gridTemplateColumns: `repeat(${Math.max(n, 1)}, minmax(0, 1fr))` }}>
        {container && container.to > container.from && (
          <div
            className="wb__container"
            style={{
              left: `${((container.from + 0.5) / n) * 100}%`,
              width: `${((container.to - container.from) / n) * 100}%`,
              height: pct(container.level),
            }}
          />
        )}
        {heights.map((h, i) => {
          const role = i === left ? 'left' : i === right ? 'right' : '';
          return (
            <div key={i} className={`wb__col${role ? ` is-${role}` : ''}${settled?.(i) ? ' is-settled' : ''}`}>
              {water && water[i] > 0 && <div className="wb__water" style={{ height: pct(water[i]) }} title={`${water[i]} water`} />}
              <div className="wb__bar" style={{ height: pct(h) }} />
              {role && <span className={`ts-tag ts-tag--${role} wb__tag`}>{role === 'left' ? 'L' : 'R'}</span>}
            </div>
          );
        })}
      </div>
      <div className="wb__axis" style={{ gridTemplateColumns: `repeat(${Math.max(n, 1)}, minmax(0, 1fr))` }}>
        {heights.map((h, i) => (
          <span key={i}>
            <b>{h}</b>
            <small>{i}</small>
          </span>
        ))}
      </div>
    </div>
  );
}
