import type { ReactNode } from 'react';

/** A stack frame: method name on top, locals below. Primitives show values; references show a dot-arrow. */
export function StackFrame({ method, locals, active }: { method: string; locals: { name: string; value: ReactNode; ref?: boolean }[]; active?: boolean }) {
  return (
    <div className={`k-frame k-sticker${active ? ' is-active' : ''}`}>
      <div className="k-frame__method">{method}</div>
      {locals.map((l) => (
        <div key={l.name} className="k-frame__local">
          <span className="k-frame__name">{l.name}</span>
          <span className={l.ref ? 'k-frame__ref' : 'k-frame__val'}>{l.ref ? <>&#9679;&#8594; {l.value}</> : l.value}</span>
        </div>
      ))}
    </div>
  );
}

/** An object on the heap: a soft blob with its class name and fields. */
export function HeapBlob({ type, fields = [], color = 'var(--pink)', id }: { type: string; fields?: { name: string; value: ReactNode }[]; color?: string; id?: string }) {
  return (
    <div className="k-heapblob k-sticker" style={{ background: color }} id={id}>
      <div className="k-heapblob__type">{type}</div>
      {fields.map((f) => (
        <div key={f.name} className="k-heapblob__field">
          <span>{f.name}</span> <b>{f.value}</b>
        </div>
      ))}
    </div>
  );
}

/** Input goes in, something happens, output comes out. For pipelines like hash -> spread -> mask. */
export function Machine({ label, input, output, children }: { label: string; input: ReactNode; output: ReactNode; children?: ReactNode }) {
  return (
    <div className="k-machine">
      <div className="k-machine__in k-sticker">{input}</div>
      <span className="k-machine__pipe" aria-hidden="true" />
      <div className="k-machine__box k-sticker">
        <svg viewBox="0 0 40 40" width="34" height="34" aria-hidden="true" className="k-machine__gear">
          <path
            d="M20 4l3 5 6-2 1 6 6 2-3 5 3 5-6 2-1 6-6-2-3 5-3-5-6 2-1-6-6-2 3-5-3-5 6-2 1-6 6 2z"
            fill="var(--amber)"
            stroke="#2C2C2A"
            strokeWidth="2.4"
            strokeLinejoin="round"
          />
          <circle cx="20" cy="20" r="5" fill="#fff" stroke="#2C2C2A" strokeWidth="2.4" />
        </svg>
        <div className="k-machine__label">{label}</div>
        {children}
      </div>
      <span className="k-machine__pipe" aria-hidden="true" />
      <div className="k-machine__out k-sticker">{output}</div>
    </div>
  );
}
