import type { ReactNode } from 'react';

/** A U-shaped cup with its index underneath. Items stack inside (a chain). */
export function Bucket({ index, highlight, children, compact }: { index: number; highlight?: boolean; children?: ReactNode; compact?: boolean }) {
  return (
    <div className={`k-bucket${highlight ? ' is-hot' : ''}${compact ? ' is-compact' : ''}`}>
      <div className="k-bucket__cup" aria-label={`bucket ${index}`}>
        {children}
      </div>
      <div className="k-bucket__index">{index}</div>
    </div>
  );
}
