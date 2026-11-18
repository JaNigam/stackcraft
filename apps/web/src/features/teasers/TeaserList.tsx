import { useState } from 'react';
import type { Teaser, TeaserResponse } from '@/shared/api/types';
import { newAttemptId } from '@/shared/api/http';
import { Button, Badge } from '@/shared/ui';
import { CodeBlock } from '@/shared/ui/CodeBlock';
import { Markdownish } from '@/shared/ui/Markdownish';
import { Character } from '@/shared/kit';
import { useCommitTeaser } from '@/features/progress/hooks';

/** "What happens if…": pick, lock it in, then the reveal. No peeking: the reveal only exists after the commit. */
function TeaserCard({ chapterId, teaser, index }: { chapterId: string; teaser: Teaser; index: number }) {
  const [picked, setPicked] = useState<number | null>(null);
  const [result, setResult] = useState<TeaserResponse | null>(null);
  const commit = useCommitTeaser(chapterId, teaser.id);

  const lockIn = async () => {
    if (picked === null) return;
    setResult(await commit.mutateAsync({ attemptId: newAttemptId(), choice: picked }));
  };

  return (
    <article className="tz-card">
      <div className="tz-card__mascot" aria-hidden="true">
        <Character kind="blob" mood={result ? (result.correct ? 'happy' : 'surprised') : 'smug'} size={64} />
      </div>
      <div className="tz-card__main">
        <header className="tz-card__head">
          <span className="qz-num">{index}</span>
          <Badge tone="pink">What happens if…</Badge>
        </header>
        <p className="tz-prompt">{teaser.prompt}</p>
        {teaser.code && <CodeBlock code={teaser.code} />}
        <div className="qz-options" role="radiogroup" aria-label="Your guess">
          {teaser.options.map((o, i) => {
            const state = result ? (i === result.answer ? ' is-right' : i === picked ? ' is-wrong' : '') : picked === i ? ' is-picked' : '';
            return (
              <Button key={o} role="radio" aria-checked={picked === i} className={`qz-option${state}`} disabled={!!result} onClick={() => setPicked(i)}>
                <span className="qz-option__letter">{String.fromCharCode(65 + i)}</span> {o}
              </Button>
            );
          })}
        </div>
        {!result ? (
          <Button variant="primary" onClick={lockIn} disabled={picked === null || commit.isPending}>
            Lock it in
          </Button>
        ) : (
          <div className={`qz-result qz-result--${result.correct ? 'ok' : 'bad'}`} role="status">
            <div className="qz-result__head">
              <strong>{result.correct ? 'You called it.' : 'Gotcha. Added to your Trap Board.'}</strong>
              {result.xpAwarded > 0 && <span className="qz-xp">+{result.xpAwarded} XP</span>}
              {result.duplicate && <span className="qz-dup">already counted</span>}
            </div>
            <Markdownish text={result.reveal} className="qz-result__why" />
          </div>
        )}
      </div>
    </article>
  );
}

export function TeaserList({ chapterId, teasers }: { chapterId: string; teasers: Teaser[] }) {
  return (
    <div className="tz-list">
      <p className="tz-intro">Commit to an answer before the reveal. Wrong guesses are not failures: they go on your Trap Board and come back later until you stop falling for them.</p>
      {teasers.map((t, i) => (
        <TeaserCard key={t.id} chapterId={chapterId} teaser={t} index={i + 1} />
      ))}
    </div>
  );
}
