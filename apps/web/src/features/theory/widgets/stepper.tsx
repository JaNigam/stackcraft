import { useEffect, useState } from 'react';
import { Button } from '@/shared/ui';

/**
 * Shared pieces for "step through an algorithm" playgrounds: the step/play state and the Reset/Back/Next/Play row.
 * Each algorithm only supplies its frames (a pure function, tested against the Java trace) and how to draw one.
 */
export function useStepper(frameCount: number, resetKey: unknown) {
  const [step, setStep] = useState(0);
  const [playing, setPlaying] = useState(false);
  const last = Math.max(0, frameCount - 1);

  useEffect(() => {
    setStep(0);
    setPlaying(false);
  }, [resetKey]);

  useEffect(() => {
    if (!playing) return;
    if (step >= last) {
      setPlaying(false);
      return;
    }
    const t = setTimeout(() => setStep((s) => s + 1), 900);
    return () => clearTimeout(t);
  }, [playing, step, last]);

  return { step: Math.min(step, last), last, playing, setStep, setPlaying };
}

export function StepperNav({ stepper }: { stepper: ReturnType<typeof useStepper> }) {
  const { step, last, playing, setStep, setPlaying } = stepper;
  return (
    <div className="qz-row ts-nav">
      <Button onClick={() => { setPlaying(false); setStep(0); }} disabled={step === 0}>
        Reset
      </Button>
      <Button onClick={() => { setPlaying(false); setStep(Math.max(0, step - 1)); }} disabled={step === 0}>
        Back
      </Button>
      <Button variant="primary" onClick={() => { setPlaying(false); setStep(Math.min(last, step + 1)); }} disabled={step >= last}>
        Next
      </Button>
      <Button onClick={() => (step >= last ? (setStep(0), setPlaying(true)) : setPlaying(!playing))}>{playing ? 'Pause' : 'Play'}</Button>
    </div>
  );
}

/** "-1, 0 1,2" -> [-1, 0, 1, 2]; null if there are too many values or any is not an integer in range. */
export function parseIntList(text: string, opts: { max: number; min: number; maxLen: number }): number[] | null {
  const parts = text.split(/[\s,]+/).filter(Boolean);
  if (parts.length > opts.maxLen) return null;
  const nums = parts.map(Number);
  return nums.every((n) => Number.isInteger(n) && n >= opts.min && n <= opts.max) ? nums : null;
}
