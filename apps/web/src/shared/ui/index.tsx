import { useId, useState, type ButtonHTMLAttributes, type ReactNode } from 'react';

/** Generic UI atoms. No business logic and no data fetching in here. */

export function Button({ variant = 'default', className = '', ...rest }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'default' | 'primary' | 'ghost' }) {
  return <button type="button" className={`ui-btn ui-btn--${variant} ${className}`} {...rest} />;
}

export function Card({ children, className = '', tone }: { children: ReactNode; className?: string; tone?: 'mint' | 'lavender' | 'pink' | 'amber' | 'sky' }) {
  return <div className={`ui-card ${tone ? `ui-card--${tone}` : ''} ${className}`}>{children}</div>;
}

export function Badge({ children, tone = 'lavender' }: { children: ReactNode; tone?: 'mint' | 'lavender' | 'pink' | 'amber' | 'sky' | 'ok' | 'bad' }) {
  return <span className={`ui-badge ui-badge--${tone}`}>{children}</span>;
}

type CalloutKind = 'trap' | 'java' | 'spring' | 'why' | 'interview';
const calloutTitle: Record<CalloutKind, string> = {
  trap: 'Trap',
  java: 'Java nudge',
  spring: 'Spring nudge',
  why: 'Why it matters',
  interview: 'Interviewer says',
};

export function Callout({ kind = 'java', title, children }: { kind?: CalloutKind; title?: string; children: ReactNode }) {
  return (
    <aside className={`ui-callout ui-callout--${kind}`}>
      <div className="ui-callout__title">{title ?? calloutTitle[kind]}</div>
      <div className="ui-callout__body">{children}</div>
    </aside>
  );
}

export interface TabDef {
  id: string;
  label: ReactNode;
  content: ReactNode;
}

/** Accessible tabs (roving arrow keys). */
export function Tabs({ tabs, initial, onChange }: { tabs: TabDef[]; initial?: string; onChange?: (id: string) => void }) {
  const [active, setActive] = useState(initial ?? tabs[0]?.id);
  const base = useId();
  const select = (id: string) => {
    setActive(id);
    onChange?.(id);
  };
  return (
    <div className="ui-tabs">
      <div role="tablist" className="ui-tabs__list" aria-label="Chapter sections">
        {tabs.map((t, i) => (
          <button
            key={t.id}
            role="tab"
            type="button"
            id={`${base}-tab-${t.id}`}
            aria-selected={active === t.id}
            aria-controls={`${base}-panel-${t.id}`}
            tabIndex={active === t.id ? 0 : -1}
            className="ui-tabs__tab"
            onClick={() => select(t.id)}
            onKeyDown={(e) => {
              if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
                const next = tabs[(i + (e.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
                select(next.id);
                document.getElementById(`${base}-tab-${next.id}`)?.focus();
              }
            }}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tabs.map((t) => (
        <div key={t.id} role="tabpanel" id={`${base}-panel-${t.id}`} aria-labelledby={`${base}-tab-${t.id}`} hidden={active !== t.id} className="ui-tabs__panel">
          {active === t.id && t.content}
        </div>
      ))}
    </div>
  );
}

export function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="ui-spinner" role="status">
      <span className="ui-spinner__dot" />
      <span className="ui-spinner__dot" />
      <span className="ui-spinner__dot" />
      <span className="sr-only">{label}</span>
    </div>
  );
}

export function ErrorNote({ error }: { error: unknown }) {
  return (
    <div className="ui-error" role="alert">
      Something went wrong: {error instanceof Error ? error.message : String(error)}
    </div>
  );
}
