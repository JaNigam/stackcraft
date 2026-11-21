import { useMemo, useState, type FormEvent } from 'react';
import { Character, Playground, type Mood } from '@/shared/kit';
import { Button } from '@/shared/ui';
import { Bars } from './Bars';
import { containerExplain, containerFrames, containerTraceLine, type ContainerFrame } from './containerFrames';
import { StepperNav, parseIntList, useStepper } from './stepper';

const PRESETS = [
  { label: 'The example', heights: [1, 8, 6, 2, 5, 4, 8, 3, 7] },
  { label: 'Equal ends', heights: [4, 3, 2, 1, 4] },
  { label: 'Trap for "always move left"', heights: [5, 1, 1, 1, 1, 9, 4] },
  { label: 'Staircase', heights: [1, 2, 3, 4, 5, 6] },
];

const LIMITS = { min: 0, max: 20, maxLen: 14 };

function mood(f: ContainerFrame): Mood {
  if (f.kind === 'done') return 'happy';
  if (f.kind === 'measure' && f.area === f.best && f.area > 0) return 'happy';
  return f.kind === 'measure' ? 'smug' : 'happy';
}

/**
 * Container With Most Water, one container at a time: the two walls, the water level (the shorter wall), the area,
 * the best so far, and why the shorter wall is the one to discard. The trace line is exactly the Java program's.
 */
export function ContainerPlayground() {
  const [heights, setHeights] = useState(PRESETS[0].heights);
  const [draft, setDraft] = useState(PRESETS[0].heights.join(', '));
  const [error, setError] = useState<string | null>(null);
  const frames = useMemo(() => containerFrames(heights), [heights]);
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

  const line = containerTraceLine(f);
  const showBest = f.kind === 'done' && f.bestPair;
  return (
    <Playground
      title="Move the shorter wall"
      hint="Each step measures one container, then throws away the shorter wall. Watch the best area: it is found at step 3 and never beaten."
      controls={
        <>
          <form className="cp-form" onSubmit={submit}>
            <label className="sr-only" htmlFor="cw-heights">
              Wall heights
            </label>
            <input id="cw-heights" className="ui-input ts-input" value={draft} onChange={(e) => setDraft(e.target.value)} />
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
            Step {stepper.step + 1} / {frames.length}: {containerExplain(f)}
          </div>
          {line && <div className="ts-trace">{line}</div>}
        </>
      }
    >
      <div className="ts-stage">
        <div className="ts-mascot" aria-hidden="true">
          <Character kind="queue" mood={mood(f)} size={60} bump={stepper.step} />
        </div>
        <Bars
          heights={heights}
          thin
          left={f.kind === 'measure' ? f.left : showBest ? f.bestPair![0] : undefined}
          right={f.kind === 'measure' ? f.right : showBest ? f.bestPair![1] : undefined}
          container={
            f.kind === 'measure'
              ? { from: f.left, to: f.right, level: f.level }
              : showBest
                ? { from: f.bestPair![0], to: f.bestPair![1], level: Math.min(heights[f.bestPair![0]], heights[f.bestPair![1]]) }
                : undefined
          }
          settled={(i) => f.kind === 'measure' && (i < f.left || i > f.right)}
          label={`Walls ${heights.join(', ')}`}
        />
        <div className="ts-sum">
          {f.kind === 'measure' ? (
            <>
              min({heights[f.left]}, {heights[f.right]}) × {f.width} = <strong className={f.area === f.best && f.area > 0 ? 'is-zero' : ''}>{f.area}</strong>
            </>
          ) : (
            <span className="k-muted">{f.kind === 'done' ? `best ${f.best}` : 'no container measured yet'}</span>
          )}
        </div>
        <div className="ts-meter">
          Best so far: <strong>{f.best}</strong>
          {f.bestPair && ` (walls ${f.bestPair[0]} and ${f.bestPair[1]})`} · containers measured:{' '}
          <strong>{frames.slice(0, stepper.step + 1).filter((x) => x.kind === 'measure').length}</strong> of {Math.max(0, (heights.length * (heights.length - 1)) / 2)} pairs
        </div>
      </div>
      <StepperNav stepper={stepper} />
    </Playground>
  );
}
