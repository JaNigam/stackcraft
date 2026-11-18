import { Button } from '@/shared/ui';

/** Honest self-grading after seeing the model answer. */
export function SelfScore({ onScore, disabled }: { onScore: (s: 0 | 1 | 2) => void; disabled?: boolean }) {
  return (
    <div className="qz-self" role="group" aria-label="How did you do?">
      <span className="qz-label">Compared with the model answer, you…</span>
      <div className="qz-row">
        <Button className="qz-self__0" onClick={() => onScore(0)} disabled={disabled}>
          Missed it
        </Button>
        <Button className="qz-self__1" onClick={() => onScore(1)} disabled={disabled}>
          Partly
        </Button>
        <Button className="qz-self__2" variant="primary" onClick={() => onScore(2)} disabled={disabled}>
          Nailed it
        </Button>
      </div>
    </div>
  );
}
