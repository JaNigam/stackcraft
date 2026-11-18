import { useParams, Link } from 'react-router';
import { useChapter } from '@/entities/chapter/hooks';
import { TheoryView } from '@/features/theory/TheoryView';
import { TeaserList } from '@/features/teasers/TeaserList';
import { QuestionList } from '@/features/quiz/QuestionList';
import { useCompleteChapter, useCompleted } from '@/features/progress/hooks';
import { newAttemptId } from '@/shared/api/http';
import { Badge, Button, ErrorNote, Spinner, Tabs } from '@/shared/ui';

function CompleteButton({ chapterId, xp }: { chapterId: string; xp: number }) {
  const done = useCompleted(chapterId);
  const complete = useCompleteChapter(chapterId);
  if (done) return <Badge tone="ok">Lesson complete</Badge>;
  return (
    <Button variant="primary" onClick={() => complete.mutate(newAttemptId())} disabled={complete.isPending}>
      I understood it: mark complete +{xp} XP
    </Button>
  );
}

export function ChapterPage() {
  const { chapterId = '' } = useParams();
  const chapter = useChapter(chapterId);
  if (chapter.isLoading) return <Spinner label="Loading chapter" />;
  if (chapter.error) return <ErrorNote error={chapter.error} />;
  const c = chapter.data!;
  const m = c.meta;
  const unitNum = Number(m.unitId.replace(/\D/g, ''));

  return (
    <div className="chapter">
      <nav className="chapter__crumbs" aria-label="Breadcrumb">
        <Link to={`/series/${m.seriesId}`}>Java Core</Link> <span aria-hidden="true">›</span> Unit {unitNum}
      </nav>
      <header className="chapter__head">
        <span className="chapter__lesson">
          Lesson {unitNum}.{m.lesson}
        </span>
        <h1>{m.title}</h1>
        {m.subtitle && <p className="chapter__subtitle">{m.subtitle}</p>}
        <div className="qz-row">
          <Badge tone="sky">{m.minutes} min</Badge>
          <Badge tone="amber">{m.completionXp} XP for the lesson</Badge>
        </div>
        {m.whereAsked.length > 0 && (
          <div className="chapter__asked">
            <span className="chapter__asked-label">Where it is asked</span>
            <ul>
              {m.whereAsked.map((w) => (
                <li key={w}>{w}</li>
              ))}
            </ul>
          </div>
        )}
      </header>
      <Tabs
        tabs={[
          {
            id: 'theory',
            label: '1 · Theory',
            content: (
              <>
                <TheoryView chapterId={m.id} />
                <div className="chapter__complete">
                  <CompleteButton chapterId={m.id} xp={m.completionXp} />
                </div>
              </>
            ),
          },
          { id: 'teasers', label: `2 · Brain teasers (${c.teasers.length})`, content: <TeaserList chapterId={m.id} teasers={c.teasers} /> },
          { id: 'questions', label: `3 · Question bank (${c.questions.length})`, content: <QuestionList chapterId={m.id} questions={c.questions} /> },
        ]}
      />
    </div>
  );
}
