import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { Character, Playground, type Mood } from '@/shared/kit';
import { Button } from '@/shared/ui';
import { bruteForceChecks, explain, threeSumFrames, traceLine, type Frame } from './threeSumFrames';

const PRESETS: { label: string; nums: number[] }[] = [
  { label: 'The example', nums: [-1, 0, 1, 2, -1, -4] },
  { label: 'All zeros', nums: [0, 0, 0, 0] },
  { label: 'Duplicates', nums: [-2, 0, 0, 2, 2] },
  { label: 'Bigger', nums: [-4, -2, -2, -1, 0, 1, 2, 3, 3, 4] },
];

const MAX = 12;

/** "-1, 0, 1, 2" -> numbers; null if anything is not a small integer. */
export function parseNumbers(text: string): number[] | null {
  const parts = text.split(/[\s,]+/).filter(Boolean);
  if (parts.length > MAX) return null;
  const nums = parts.map(Number);
  return nums.every((n) => Number.isInteger(n) && Math.abs(n) <= 99) ? nums : null;
}

const moodFor: Record<Frame['kind'], Mood> = {
  sorted: 'happy',
  captain: 'smug',
  'skip-captain': 'sleepy',
  'stop-positive': 'nope',
  'too-small': 'surprised',
  'too-big': 'surprised',
  found: 'happy',
  done: 'happy',
};

function Tile({ value, index, f }: { value: number; index: number; f: Frame }) {
  const reduce = useReducedMotion();
  const role = index === f.i ? 'captain' : index === f.left ? 'left' : index === f.right ? 'right' : '';
  const settled = f.i >= 0 && index < f.i && f.kind !== 'done';
  const inFound = f.kind === 'found' && (index === f.i || index === f.left || index === f.right);
  return (
    <div className={`ts-tile${role ? ` is-${role}` : ''}${settled ? ' is-settled' : ''}${inFound ? ' is-hit' : ''}`}>
      <motion.span
        className="ts-tile__value"
        animate={reduce ? undefined : inFound ? { y: [0, -8, 0] } : { y: 0 }}
        transition={{ duration: 0.35 }}
      >
        {value}
      </motion.span>
      <span className="ts-tile__index">{index}</span>
      {role && <span className={`ts-tag ts-tag--${role}`}>{role === 'captain' ? 'C' : role === 'left' ? 'L' : 'R'}</span>}
    </div>
  );
}

/**
 * Step through 3Sum on any small array: sorted tiles, the captain, both pointers, the running sum, and every
 * decision explained. The trace line under the stage is exactly what ThreeSum.java prints.
 */
export function ThreeSumPlayground({ initial = [-1, 0, 1, 2, -1, -4] }: { initial?: number[] }) {
  const [nums, setNums] = useState(initial);
  const [draft, setDraft] = useState(initial.join(', '));
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState(0);
  const [playing, setPlaying] = useState(false);
  const frames = useMemo(() => threeSumFrames(nums), [nums]);
  const f = frames[Math.min(step, frames.length - 1)];
  const last = frames.length - 1;

  useEffect(() => {
    if (!playing) return;
    if (step >= last) {
      setPlaying(false);
      return;
    }
    const t = setTimeout(() => setStep((s) => s + 1), 900);
    return () => clearTimeout(t);
  }, [playing, step, last]);

  const load = (next: number[]) => {
    setNums(next);
    setDraft(next.join(', '));
    setError(null);
    setStep(0);
    setPlaying(false);
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const parsed = parseNumbers(draft);
    if (!parsed) {
      setError(`Up to ${MAX} whole numbers between -99 and 99, separated by commas`);
      return;
    }
    load(parsed);
  };

  const line = traceLine(f);
  return (
    <Playground
      title="3Sum, one decision at a time"
      hint="Press Next to watch the captain and the two pointers. Try the duplicates preset: no team is recorded twice."
      controls={
        <>
          <form className="cp-form" onSubmit={submit}>
            <label className="sr-only" htmlFor="ts-nums">
              Numbers
            </label>
            <input id="ts-nums" className="ui-input ts-input" value={draft} onChange={(e) => setDraft(e.target.value)} />
            <Button type="submit">Use these</Button>
          </form>
          <div className="cp-chips">
            {PRESETS.map((p) => (
              <Button key={p.label} onClick={() => load(p.nums)}>
                {p.label}
              </Button>
            ))}
          </div>
          {error && (
            <p className="auth__error" role="alert">
              {error}
            </p>
          )}
        </>
      }
      readout={
        <>
          <div>
            Step {step + 1} / {frames.length}: {explain(f)}
          </div>
          {line && <div className="ts-trace">{line}</div>}
        </>
      }
    >
      <div className="ts-stage">
        <div className="ts-mascot" aria-hidden="true">
          <Character kind="queue" mood={moodFor[f.kind]} size={64} bump={step} />
        </div>
        <div className="ts-tiles" role="img" aria-label={`Sorted numbers ${f.sorted.join(', ')}`}>
          {f.sorted.map((v, idx) => (
            <Tile key={idx} value={v} index={idx} f={f} />
          ))}
        </div>
        <div className="ts-sum" aria-live="off">
          {f.sum !== null ? (
            <>
              {f.sorted[f.i]} + {f.sorted[f.left]} + {f.sorted[f.right]} = <strong className={f.sum === 0 ? 'is-zero' : ''}>{f.sum}</strong>
            </>
          ) : (
            <span className="k-muted">no sum yet</span>
          )}
        </div>
        <div className="ts-found">
          <span className="co-eyebrow">Found</span>
          {f.found.length === 0 ? (
            <span className="k-muted">nothing yet</span>
          ) : (
            f.found.map((t, k) => (
              <span key={k} className="k-pill is-new">
                [{t.join(', ')}]
              </span>
            ))
          )}
        </div>
        <div className="ts-meter">
          Sum checks: <strong>{f.checks}</strong> · brute force would try <strong>{bruteForceChecks(f.sorted.length)}</strong> triples
        </div>
      </div>
      <div className="qz-row ts-nav">
        <Button onClick={() => { setPlaying(false); setStep(0); }} disabled={step === 0}>
          Reset
        </Button>
        <Button onClick={() => { setPlaying(false); setStep((s) => Math.max(0, s - 1)); }} disabled={step === 0}>
          Back
        </Button>
        <Button variant="primary" onClick={() => { setPlaying(false); setStep((s) => Math.min(last, s + 1)); }} disabled={step >= last}>
          Next
        </Button>
        <Button onClick={() => (step >= last ? (setStep(0), setPlaying(true)) : setPlaying((p) => !p))}>{playing ? 'Pause' : 'Play'}</Button>
      </div>
    </Playground>
  );
}
