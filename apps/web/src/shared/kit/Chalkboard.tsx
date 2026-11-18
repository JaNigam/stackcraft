import type { ReactNode } from 'react';

/**
 * Chalkboard panel: ONLY for exact internals (real field names, array slots, bit maths). Mono chalk on green.
 * Everything else uses stickers.
 */
export function Chalkboard({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <section className="k-board" aria-label={title ?? 'Chalkboard'}>
      {title && <h4 className="k-board__title">{title}</h4>}
      <div className="k-board__body">{children}</div>
    </section>
  );
}

export function Chalk({ color = 'white', children }: { color?: 'white' | 'yellow' | 'pink' | 'mint'; children: ReactNode }) {
  return <span className={`k-chalk k-chalk--${color}`}>{children}</span>;
}
