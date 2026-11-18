import type { ReactNode } from 'react';

export interface NodeField {
  name: string;
  value: ReactNode;
}

/** One object box with named fields, sticker style. Use inside <Chain> for linked structures. */
export function NodeBox({ title, fields, color = 'var(--lavender)' }: { title?: string; fields: NodeField[]; color?: string }) {
  return (
    <div className="k-node k-sticker" style={{ background: color }}>
      {title && <div className="k-node__title">{title}</div>}
      <dl className="k-node__fields">
        {fields.map((f) => (
          <div key={f.name} className="k-node__row">
            <dt>{f.name}</dt>
            <dd>{f.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

/** Items joined by hand-drawn arrows; ends with a null sticker when `terminated`. */
export function Chain({ children, terminated = true, direction = 'row' }: { children: ReactNode[]; terminated?: boolean; direction?: 'row' | 'column' }) {
  return (
    <div className={`k-chain k-chain--${direction}`}>
      {children.map((child, i) => (
        <div key={i} className="k-chain__link">
          {child}
          {(i < children.length - 1 || terminated) && <span className="k-chain__arrow" aria-hidden="true" />}
        </div>
      ))}
      {terminated && <span className="k-chain__null">null</span>}
    </div>
  );
}
