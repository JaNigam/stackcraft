import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useParams, Link } from 'react-router';
import { useReducedMotion } from 'framer-motion';
import { useChapter } from '@/entities/chapter/hooks';
import { TheoryView } from '@/features/theory/TheoryView';
import { TeaserList } from '@/features/teasers/TeaserList';
import { QuestionList } from '@/features/quiz/QuestionList';
import { ChapterOutline, ChapterOutlineSheet, type ChapterTab } from '@/features/chapter-outline/ChapterOutline';
import { scrollToWhenReady } from '@/features/chapter-outline/useOutline';
import { useCompleteChapter, useCompleted } from '@/features/progress/hooks';
import { newAttemptId } from '@/shared/api/http';
import type { ChapterView } from '@/shared/api/types';
import { Badge, Button, ErrorNote, Spinner, Tabs } from '@/shared/ui';

const TABS: ChapterTab[] = ['theory', 'teasers', 'questions'];

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

/** "#theory/how-a-hashset-picks-the-bucket" or "#questions/tier-killer" or "#teasers". */
function parseHash(hash: string): { tab: ChapterTab; id?: string } | null {
  const [tab, id] = hash.replace(/^#/, '').split('/');
  return TABS.includes(tab as ChapterTab) ? { tab: tab as ChapterTab, id: id || undefined } : null;
}

function ChapterBody({ c }: { c: ChapterView }) {
  const m = c.meta;
  const unitNum = Number(m.unitId.replace(/\D/g, ''));
  const [tab, setTab] = useState<ChapterTab>(() => parseHash(location.hash)?.tab ?? 'theory');
  const [jump, setJump] = useState<{ id?: string; smooth: boolean; n: number }>(() => ({ id: parseHash(location.hash)?.id, smooth: false, n: 0 }));
  const tabsRef = useRef<HTMLDivElement>(null);
  const reduce = useReducedMotion();

  /** Switch tab if needed; the layout effect below scrolls once the tab is on screen. */
  const navigate = useCallback(
    (next: ChapterTab, id?: string) => {
      setTab(next);
      setJump((j) => ({ id, smooth: !reduce, n: j.n + 1 }));
      history.replaceState(null, '', `#${next}${id ? `/${id}` : ''}`);
    },
    [reduce],
  );

  // Runs right after React shows the new tab (no frame timing involved). Falls back to polling only when the target
  // is not on screen yet, e.g. a deep link into theory that is still lazy-loading.
  useLayoutEffect(() => {
    if (jump.n === 0 && !jump.id) return;
    if (!jump.id) {
      tabsRef.current?.scrollIntoView({ behavior: jump.smooth ? 'smooth' : 'auto', block: 'start' });
      return;
    }
    scrollToWhenReady(jump.id, jump.smooth);
  }, [jump]);

  // A deep link must win over the browser restoring the old scroll position on reload.
  useEffect(() => {
    if (!parseHash(location.hash)?.id) return;
    const previous = history.scrollRestoration;
    history.scrollRestoration = 'manual';
    return () => {
      history.scrollRestoration = previous;
    };
  }, []);

  const onTabChange = (id: string) => {
    setTab(id as ChapterTab);
    history.replaceState(null, '', `#${id}`);
  };

  const outline = <ChapterOutline chapter={c} activeTab={tab} onNavigate={navigate} />;

  return (
    <div className="chapter chapter--with-outline">
      <div className="chapter__main">
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
        <div ref={tabsRef} className="chapter__tabs">
          <Tabs
            active={tab}
            onChange={onTabChange}
            keepMounted
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
      </div>
      <aside className="chapter__outline" aria-label="Chapter summary">
        {outline}
      </aside>
      <ChapterOutlineSheet chapter={c} activeTab={tab} onNavigate={navigate} />
    </div>
  );
}

export function ChapterPage() {
  const { chapterId = '' } = useParams();
  const chapter = useChapter(chapterId);
  if (chapter.isLoading) return <Spinner label="Loading chapter" />;
  if (chapter.error) return <ErrorNote error={chapter.error} />;
  return <ChapterBody key={chapterId} c={chapter.data!} />;
}
