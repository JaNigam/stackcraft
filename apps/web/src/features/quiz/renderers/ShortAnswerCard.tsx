import { useState } from 'react';
import type { AnswerResponse } from '@/shared/api/types';
import { Button } from '@/shared/ui';
import { Markdownish } from '@/shared/ui/Markdownish';
import type { QuestionRendererProps } from '../types';
import { ResultBanner } from '../ResultBanner';
import { SelfScore } from './SelfScore';

/** Say (or type) your answer first, then compare with the model answer and score yourself. */
export function ShortAnswerCard({ question, onSubmit }: QuestionRendererProps) {
  const [draft, setDraft] = useState('');
  const [revealed, setRevealed] = useState(false);
  const [result, setResult] = useState<AnswerResponse | null>(null);
  const [busy, setBusy] = useState(false);

  const score = async (selfScore: 0 | 1 | 2) => {
    setBusy(true);
    try {
      setResult(await onSubmit({ selfScore, text: draft || undefined }));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="qz-body">
      <label className="qz-label" htmlFor={`sa-${question.id}`}>
        Answer out loud first. Notes (optional, not graded):
      </label>
      <textarea id={`sa-${question.id}`} className="ui-input" rows={3} value={draft} onChange={(e) => setDraft(e.target.value)} />
      {!revealed ? (
        <Button variant="primary" onClick={() => setRevealed(true)}>
          Show the model answer
        </Button>
      ) : (
        <div className="qz-model">
          <span className="qz-result__label">Model answer</span>
          <Markdownish text={question.modelAnswer ?? ''} />
          {question.keyPoints && question.keyPoints.length > 0 && (
            <>
              <span className="qz-result__label">Key points to hit</span>
              <ul className="qz-points">
                {question.keyPoints.map((k) => (
                  <li key={k}>{k}</li>
                ))}
              </ul>
            </>
          )}
          {!result ? <SelfScore onScore={score} disabled={busy} /> : <ResultBanner result={{ ...result, explanation: '' }} />}
        </div>
      )}
    </div>
  );
}
