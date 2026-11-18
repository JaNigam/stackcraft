import { useState } from 'react';
import type { AnswerResponse } from '@/shared/api/types';
import { Button } from '@/shared/ui';
import { CodeBlock } from '@/shared/ui/CodeBlock';
import type { QuestionRendererProps } from '../types';
import { ResultBanner } from '../ResultBanner';

export function McqCard({ question, onSubmit }: QuestionRendererProps) {
  const [picked, setPicked] = useState<number | null>(null);
  const [result, setResult] = useState<AnswerResponse | null>(null);
  const [busy, setBusy] = useState(false);

  const choose = async (i: number) => {
    if (result || busy) return;
    setPicked(i);
    setBusy(true);
    try {
      setResult(await onSubmit({ choice: i }));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="qz-body">
      {question.code && <CodeBlock code={question.code} />}
      <div className="qz-options" role="group" aria-label="Options">
        {question.options?.map((o, i) => {
          const state = result ? (i === question.answer ? ' is-right' : i === picked ? ' is-wrong' : '') : picked === i ? ' is-picked' : '';
          return (
            <Button key={o} className={`qz-option${state}`} onClick={() => choose(i)} disabled={busy || (result !== null && i !== picked && i !== question.answer)} aria-pressed={picked === i}>
              <span className="qz-option__letter">{String.fromCharCode(65 + i)}</span> {o}
            </Button>
          );
        })}
      </div>
      {result && (
        <>
          <ResultBanner result={result} />
          <Button variant="ghost" onClick={() => { setResult(null); setPicked(null); }}>
            Try again
          </Button>
        </>
      )}
    </div>
  );
}
