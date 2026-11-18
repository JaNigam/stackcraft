import { Link } from 'react-router';
import { useTrapBoard } from '@/features/progress/hooks';
import { Character } from '@/shared/kit';
import { Badge, ErrorNote, Spinner } from '@/shared/ui';

export function TrapBoardPage() {
  const traps = useTrapBoard();
  if (traps.isLoading) return <Spinner />;
  if (traps.error) return <ErrorNote error={traps.error} />;
  const list = traps.data ?? [];
  return (
    <div className="traps">
      <h1>Trap Board</h1>
      <p>Every gotcha you fell for. Each one stays here until you answer it right.</p>
      {list.length === 0 ? (
        <div className="traps__empty">
          <Character kind="blob" mood="happy" label="No traps. Yet." />
        </div>
      ) : (
        <ul className="traps__list">
          {list.map((t) => (
            <li key={`${t.chapterId}/${t.itemId}`} className="ui-card traps__item">
              <div className="qz-row">
                <Badge tone={t.kind === 'teaser' ? 'pink' : 'sky'}>{t.kind}</Badge>
                <Badge tone="bad">fallen {t.timesFallen}×</Badge>
              </div>
              <p>{t.prompt}</p>
              <Link to={`/chapters/${t.chapterId}`}>Back to the chapter →</Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
