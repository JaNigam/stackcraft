import { lazy, Suspense, useState } from 'react';
import { useCodeRunner } from '@/app/services';
import { Badge, Button } from '@/shared/ui';
import { CodeBlock, OutputBlock } from '@/shared/ui/CodeBlock';
import { useChapterContext } from '@/features/theory/ChapterContext';
import type { RunResult } from '@/shared/api/types';
import { codeFile } from './codeFiles';

const JavaEditor = lazy(() => import('./JavaEditor'));

type View =
  | { state: 'idle' }
  | { state: 'running' }
  | { state: 'ran'; result: RunResult }
  | { state: 'fallback'; reason: string };

const statusTone = { OK: 'ok', COMPILE_ERROR: 'bad', RUNTIME_ERROR: 'bad', TIMEOUT: 'amber', OUTPUT_LIMIT: 'amber' } as const;

/**
 * A chapter code file you can read, edit and run on the live runner. If the runner is down, busy or the site is
 * offline, it shows the verified .out instead (and says so). The .out is checked against a real run in CI.
 */
export function RunnableCode({ file, title }: { file: string; title?: string }) {
  const { chapterId } = useChapterContext();
  const code = codeFile(chapterId, file);
  const runner = useCodeRunner();
  const [source, setSource] = useState(code?.source ?? '');
  const [editing, setEditing] = useState(false);
  const [view, setView] = useState<View>({ state: 'idle' });

  if (!code) return <div className="ui-error">Missing code file {file}</div>;
  const edited = source !== code.source;

  const run = async () => {
    setView({ state: 'running' });
    const outcome = await runner.run(source);
    if (outcome.kind === 'ran') setView({ state: 'ran', result: outcome.result });
    else setView({ state: 'fallback', reason: outcome.kind === 'busy' ? 'The runner is busy.' : 'The live runner is unavailable.' });
  };

  return (
    <section className="cr" aria-label={`Code: ${file}`}>
      <header className="cr__head">
        <span className="cr__file">{title ?? file}</span>
        <div className="cr__actions">
          <Button variant="ghost" onClick={() => setEditing((e) => !e)} aria-pressed={editing}>
            {editing ? 'Done editing' : 'Edit'}
          </Button>
          {edited && (
            <Button variant="ghost" onClick={() => setSource(code.source)}>
              Reset
            </Button>
          )}
          <Button variant="primary" onClick={run} disabled={view.state === 'running'}>
            {view.state === 'running' ? 'Running…' : 'Run ▶'}
          </Button>
        </div>
      </header>
      {editing ? (
        <Suspense fallback={<CodeBlock code={source} />}>
          <JavaEditor value={source} onChange={setSource} />
        </Suspense>
      ) : (
        <CodeBlock code={source} />
      )}
      <RunOutput view={view} verified={edited ? undefined : code.verifiedOutput} />
    </section>
  );
}

function RunOutput({ view, verified }: { view: View; verified?: string }) {
  if (view.state === 'idle') return null;
  if (view.state === 'running') return <p className="cr__note">Compiling and running on the sandboxed runner (10 s, 128 MB)…</p>;
  if (view.state === 'fallback') {
    return (
      <div className="cr__fallback">
        <p className="cr__note">
          <Badge tone="amber">Stored output</Badge> {view.reason} {verified ? 'Showing the verified output from the last real run.' : 'Edited code has no stored output; try again when the runner is back.'}
        </p>
        {verified && <OutputBlock text={verified} label="Verified output" />}
      </div>
    );
  }
  const r = view.result;
  return (
    <div className="cr__result">
      <p className="cr__note">
        <Badge tone={statusTone[r.status]}>{r.status.replace('_', ' ')}</Badge> in {r.durationMs} ms{r.truncated ? ' · output truncated at 64 KB' : ''}
      </p>
      {r.stdout && <OutputBlock text={r.stdout} label="stdout" />}
      {r.stderr && <OutputBlock text={r.stderr} label="stderr" />}
    </div>
  );
}
