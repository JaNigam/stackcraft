import { useState } from 'react';
import { useCodeRunner } from '@/app/services';
import type { AnswerResponse } from '@/shared/api/types';
import { Button } from '@/shared/ui';
import { CodeBlock, OutputBlock } from '@/shared/ui/CodeBlock';
import type { QuestionRendererProps } from '../types';
import { ResultBanner } from '../ResultBanner';

/** Predict the output, then (optionally) run the program on the real JVM to see it for yourself. */
export function PredictCard({ question, onSubmit }: QuestionRendererProps) {
  const [text, setText] = useState('');
  const [result, setResult] = useState<AnswerResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [live, setLive] = useState<string | null>(null);
  const runner = useCodeRunner();

  const submit = async () => {
    setBusy(true);
    try {
      setResult(await onSubmit({ text }));
    } finally {
      setBusy(false);
    }
  };

  const runIt = async () => {
    setLive('Running on the JVM…');
    const o = await runner.run(question.code ?? '');
    setLive(o.kind === 'ran' ? o.result.stdout || o.result.stderr : `${o.message} The verified output is shown above.`);
  };

  return (
    <div className="qz-body">
      <CodeBlock code={question.code ?? ''} />
      <label className="qz-label" htmlFor={`pred-${question.id}`}>
        Your predicted output (exact lines)
      </label>
      <textarea id={`pred-${question.id}`} className="ui-input" rows={Math.max(3, (question.expected ?? '').split('\n').length + 1)} value={text} onChange={(e) => setText(e.target.value)} disabled={!!result} spellCheck={false} />
      {!result ? (
        <Button variant="primary" onClick={submit} disabled={busy || text.trim() === ''}>
          Lock in my prediction
        </Button>
      ) : (
        <>
          <ResultBanner result={result} />
          <div className="qz-row">
            <Button onClick={runIt}>Run it on the JVM ▶</Button>
            <Button variant="ghost" onClick={() => { setResult(null); setLive(null); }}>
              Try again
            </Button>
          </div>
          {live && <OutputBlock text={live} label="Live run" />}
        </>
      )}
    </div>
  );
}
