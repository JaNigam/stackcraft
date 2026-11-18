import { useState, type ReactNode, type KeyboardEvent } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';

export interface Step {
  title: string;
  body: ReactNode;
}

/** Walks through a process one frame at a time. Arrow keys work when focused. */
export function Stepper({ steps, label = 'Steps' }: { steps: Step[]; label?: string }) {
  const [i, setI] = useState(0);
  const reduce = useReducedMotion();
  const go = (n: number) => setI(Math.max(0, Math.min(steps.length - 1, n)));
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'ArrowRight') go(i + 1);
    if (e.key === 'ArrowLeft') go(i - 1);
  };
  return (
    <section className="k-stepper" aria-label={label} tabIndex={0} onKeyDown={onKey}>
      <header className="k-stepper__head">
        <span className="k-stepper__count">
          Step {i + 1} / {steps.length}
        </span>
        <strong>{steps[i].title}</strong>
      </header>
      <AnimatePresence mode="wait" initial={false}>
        <motion.div
          key={i}
          className="k-stepper__body"
          initial={reduce ? false : { opacity: 0, x: 14 }}
          animate={{ opacity: 1, x: 0 }}
          exit={reduce ? undefined : { opacity: 0, x: -14 }}
          transition={{ duration: 0.18 }}
        >
          {steps[i].body}
        </motion.div>
      </AnimatePresence>
      <footer className="k-stepper__nav">
        <button type="button" className="ui-btn" onClick={() => go(i - 1)} disabled={i === 0}>
          Back
        </button>
        <div className="k-stepper__dots" role="tablist" aria-label="Jump to step">
          {steps.map((s, n) => (
            <button key={s.title} type="button" role="tab" aria-selected={n === i} aria-label={`Step ${n + 1}: ${s.title}`} className={n === i ? 'is-on' : ''} onClick={() => go(n)} />
          ))}
        </div>
        <button type="button" className="ui-btn ui-btn--primary" onClick={() => go(i + 1)} disabled={i === steps.length - 1}>
          Next
        </button>
      </footer>
    </section>
  );
}
