import { useEffect, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { rankProgress } from '@/entities/learner/ranks';
import { useStreak, useXp } from './hooks';
import { onXp } from './xpEvents';

/** Rank, XP bar to the next rank, and streak. Presentational over the progress hooks. */
export function ProgressHeader() {
  const xp = useXp();
  const streak = useStreak();
  if (!xp) return <div className="pg-head pg-head--loading" aria-hidden="true" />;
  const pct = Math.round(rankProgress(xp.progress) * 100);
  return (
    <div className="pg-head" aria-label="Your progress">
      <span className="pg-rank" title="Rank">
        {xp.rank}
      </span>
      <div className="pg-bar" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={pct} aria-label={xp.next ? `${xp.toNext} XP to ${xp.next}` : 'Top rank'}>
        <div className="pg-bar__fill" style={{ width: `${pct}%` }} />
        <span className="pg-bar__text">
          {xp.total} XP{xp.next ? ` · ${xp.toNext} to ${xp.next}` : ''}
        </span>
      </div>
      <span className={`pg-streak${streak > 0 ? ' is-lit' : ''}`} title={`${streak}-day streak`}>
        <svg viewBox="0 0 20 24" width="16" height="19" aria-hidden="true">
          <path d="M10 1 C12 6 18 9 18 15 A8 8 0 0 1 2 15 C2 11 5 9 6 5 C8 8 9 9 10 9 C10 6 9 4 10 1 Z" fill={streak > 0 ? 'var(--coral)' : 'var(--surface-2)'} stroke="#2C2C2A" strokeWidth="2" strokeLinejoin="round" />
        </svg>
        {streak}
      </span>
      <XpToast />
    </div>
  );
}

function XpToast() {
  const [toast, setToast] = useState<{ id: number; xp: number; label: string } | null>(null);
  const reduce = useReducedMotion();
  useEffect(
    () =>
      onXp((xp, label) => {
        const id = Date.now();
        setToast({ id, xp, label });
        setTimeout(() => setToast((t) => (t?.id === id ? null : t)), 1800);
      }),
    [],
  );
  return (
    <div className="pg-toast" aria-live="polite">
      <AnimatePresence>
        {toast && (
          <motion.span
            key={toast.id}
            className="pg-toast__chip"
            initial={reduce ? false : { y: 12, opacity: 0, scale: 0.8 }}
            animate={{ y: 0, opacity: 1, scale: 1 }}
            exit={reduce ? undefined : { y: -12, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 420, damping: 18 }}
          >
            +{toast.xp} XP <small>{toast.label}</small>
          </motion.span>
        )}
      </AnimatePresence>
    </div>
  );
}
