import type { ReactNode } from 'react';

/**
 * Frame for anything the learner can operate: a stage, a row of controls and a live readout. The readout is an
 * aria-live region so screen readers hear what changed.
 */
export function Playground({ title, hint, controls, readout, children }: { title: string; hint?: string; controls?: ReactNode; readout?: ReactNode; children: ReactNode }) {
  return (
    <section className="k-play" aria-label={title}>
      <header className="k-play__head">
        <span className="k-play__badge">Try it</span>
        <h4>{title}</h4>
      </header>
      {hint && <p className="k-play__hint">{hint}</p>}
      {controls && <div className="k-play__controls">{controls}</div>}
      <div className="k-play__stage">{children}</div>
      {readout && (
        <div className="k-play__readout" aria-live="polite">
          {readout}
        </div>
      )}
    </section>
  );
}
