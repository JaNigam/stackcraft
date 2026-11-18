import { useState } from 'react';
import type { AnswerResponse } from '@/shared/api/types';
import { Button } from '@/shared/ui';
import { Markdownish } from '@/shared/ui/Markdownish';
import type { QuestionRendererProps } from '../types';
import { ResultBanner } from '../ResultBanner';
import { SelfScore } from './SelfScore';

/** Killer problem: escalating hints (each costs 20% of the XP), then the model answer and a self-score. */
export function ProblemCard({ question, onSubmit }: QuestionRendererProps) {
  const hints = question.hints ?? [];
  const [shown, setShown] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [result, setResult] = useState<AnswerResponse | null>(null);
  const [busy, setBusy] = useState(false);

  const score = async (selfScore: 0 | 1 | 2) => {
    setBusy(true);
    try {
      setResult(await onSubmit({ selfScore, hintsUsed: shown }));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="qz-body">
      {shown > 0 && (
        <ol className="qz-hints">
          {hints.slice(0, shown).map((h, i) => (
            <li key={i}>
              <span className="qz-hint__tag">Hint {i + 1}</span> {h}
            </li>
          ))}
        </ol>
      )}
      {!revealed && (
        <div className="qz-row">
          {shown < hints.length && (
            <Button onClick={() => setShown((n) => n + 1)}>
              {shown === 0 ? 'I need a hint' : 'Another hint'} <small>(−20% XP)</small>
            </Button>
          )}
          <Button variant="primary" onClick={() => setRevealed(true)}>
            I have my answer: show the model
          </Button>
        </div>
      )}
      {revealed && (
        <div className="qz-model">
          <span className="qz-result__label">Model answer</span>
          <Markdownish text={question.modelAnswer ?? ''} />
          {!result ? <SelfScore onScore={score} disabled={busy} /> : <ResultBanner result={{ ...result, explanation: '' }} />}
        </div>
      )}
    </div>
  );
}
