import { useMemo, useState, type FormEvent } from 'react';
import { Character, Playground, type Mood } from '@/shared/kit';
import { Button } from '@/shared/ui';
import { Bars } from './Bars';
import { rainExplain, rainFrames, rainTraceLine, type RainFrame } from './rainFrames';
import { StepperNav, parseIntList, useStepper } from './stepper';

const PRESETS = [
  { label: 'The example', heights: [0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1] },
  { label: 'Deep pool', heights: [4, 2, 0, 3, 2, 5] },
  { label: 'Two pools', heights: [1, 0, 4, 0, 4, 0, 1] },
  { label: 'Hill (no water)', heights: [1, 2, 3, 4, 3, 2, 1] },
];

const LIMITS = { min: 0, max: 12, maxLen: 16 };

const moods: Record<RainFrame['kind'], Mood> = {
  start: 'happy',
  'new-left-max': 'smug',
  'new-right-max': 'smug',
  'water-left': 'happy',
  'water-right': 'happy',
  done: 'happy',
};

/**
 * Trapping Rain Water with two pointers: each step settles one column on the lower side, using that side's
 * running max, and pours its water in. The trace line is exactly the Java program's.
 */
export function RainWaterPlayground() {
  const [heights, setHeights] = useState(PRESETS[0].heights);
  const [draft, setDraft] = useState(PRESETS[0].heights.join(', '));
  const [error, setError] = useState<string | null>(null);
  const frames = useMemo(() => rainFrames(heights), [heights]);
  const stepper = useStepper(frames.length, heights);
  const f = frames[stepper.step];

  const load = (next: number[]) => {
    setHeights(next);
    setDraft(next.join(', '));
    setError(null);
  };
  const submit = (e: FormEvent) => {
    e.preventDefault();
    const parsed = parseIntList(draft, LIMITS);
    if (!parsed) setError(`Up to ${LIMITS.maxLen} whole numbers from 0 to ${LIMITS.max}, separated by commas`);
    else load(parsed);
  };

  const line = rainTraceLine(f);
  const live = f.kind !== 'done' && f.kind !== 'start';
  return (
    <Playground
      title="Settle the lower side"
      hint="The lower side's running max decides its water, because the other side already has a wall at least that tall."
      controls={
        <>
          <form className="cp-form" onSubmit={submit}>
            <label className="sr-only" htmlFor="rw-heights">
              Bar heights
            </label>
            <input id="rw-heights" className="ui-input ts-input" value={draft} onChange={(e) => setDraft(e.target.value)} />
            <Button type="submit">Use these</Button>
          </form>
          <div className="cp-chips">
            {PRESETS.map((p) => (
              <Button key={p.label} onClick={() => load(p.heights)}>
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
            Step {stepper.step + 1} / {frames.length}: {rainExplain(f)}
          </div>
          {line && <div className="ts-trace">{line}</div>}
        </>
      }
    >
      <div className="ts-stage">
        <div className="ts-mascot" aria-hidden="true">
          <Character kind="blob" mood={moods[f.kind]} size={60} bump={stepper.step} />
        </div>
        <Bars
          heights={heights}
          water={f.water}
          left={f.kind === 'start' || live ? f.left : undefined}
          right={f.kind === 'start' || live ? f.right : undefined}
          settled={(i) => f.done[i] && i !== f.at}
          label={`Bars ${heights.join(', ')}, water ${f.water.join(', ')}`}
        />
        <div className="ts-sum">
          leftMax <strong>{f.leftMax}</strong> · rightMax <strong>{f.rightMax}</strong> · water <strong className={f.total > 0 ? 'is-zero' : ''}>{f.total}</strong>
        </div>
        <div className="ts-meter">Each bar is settled once: {heights.length} bars, at most {Math.max(0, heights.length - 1)} steps, no extra arrays.</div>
      </div>
      <StepperNav stepper={stepper} />
    </Playground>
  );
}
