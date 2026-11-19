import { useEffect, useState } from 'react';
import type { ChapterView } from '@/shared/api/types';
import { useProgress } from '@/features/progress/hooks';
import { chapterProgress, type PartProgress } from './chapterProgress';
import { useActiveSection, useOutlineEntries } from './useOutline';

export type ChapterTab = 'theory' | 'teasers' | 'questions';

interface Props {
  chapter: ChapterView;
  activeTab: ChapterTab;
  onNavigate: (tab: ChapterTab, targetId?: string) => void;
}

const TIER_LABEL = { warmup: 'Warm-up', interview: 'Interview', killer: 'Killer' } as const;

function Count({ part }: { part: PartProgress }) {
  const full = part.total > 0 && part.done === part.total;
  return (
    <span className={`co-count${full ? ' is-full' : ''}`}>
      {part.done}/{part.total}
    </span>
  );
}

/**
 * "Where am I in this chapter?": progress for the chapter, then every section, current one highlighted. Clicking a
 * section switches tab if needed and scrolls there. Pure view over the chapter + progress data; the page owns tabs.
 */
export function ChapterOutline({ chapter, activeTab, onNavigate }: Props) {
  const progress = useProgress().data;
  const p = chapterProgress(chapter, progress);
  const theory = useOutlineEntries('.th');
  const tierIds = (['warmup', 'interview', 'killer'] as const).filter((t) => p.tiers[t].total > 0);

  const spyIds = activeTab === 'theory' ? theory.map((e) => e.id) : activeTab === 'questions' ? tierIds.map((t) => `tier-${t}`) : [];
  const active = useActiveSection(spyIds);

  return (
    <nav className="co" aria-label="Chapter outline">
      <div className="co-progress">
        <div className="co-progress__top">
          <span className="co-eyebrow">Your progress</span>
          <strong>{p.percent}%</strong>
        </div>
        <div className="co-bar" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={p.percent} aria-label="Chapter progress">
          <span style={{ width: `${p.percent}%` }} />
        </div>
        <div className="co-xp">
          {p.xpEarned} / {p.xpAvailable} XP
        </div>
        <ul className="co-checks">
          <li className={p.lessonComplete ? 'is-done' : ''}>
            <span aria-hidden="true">{p.lessonComplete ? '✓' : '○'}</span> Lesson {p.lessonComplete ? 'complete' : 'not complete yet'}
          </li>
          <li className={p.teasers.done === p.teasers.total && p.teasers.total > 0 ? 'is-done' : ''}>
            <span aria-hidden="true">✦</span> Teasers <Count part={p.teasers} />
          </li>
          <li className={p.questions.done === p.questions.total && p.questions.total > 0 ? 'is-done' : ''}>
            <span aria-hidden="true">✎</span> Questions <Count part={p.questions} />
          </li>
        </ul>
      </div>

      <ol className="co-sections">
        <li>
          <button type="button" className={`co-link co-link--tab${activeTab === 'theory' ? ' is-tab' : ''}`} onClick={() => onNavigate('theory')}>
            <span className="co-num">1</span> Theory
          </button>
          {theory.length > 0 && (
            <ol className="co-sub">
              {theory.map((e) => (
                <li key={e.id}>
                  <button
                    type="button"
                    className={`co-link${activeTab === 'theory' && active === e.id ? ' is-current' : ''}`}
                    aria-current={activeTab === 'theory' && active === e.id ? 'location' : undefined}
                    onClick={() => onNavigate('theory', e.id)}
                  >
                    {e.label}
                  </button>
                </li>
              ))}
            </ol>
          )}
        </li>
        <li>
          <button type="button" className={`co-link co-link--tab${activeTab === 'teasers' ? ' is-tab' : ''}`} onClick={() => onNavigate('teasers')}>
            <span className="co-num">2</span> Brain teasers <Count part={p.teasers} />
          </button>
        </li>
        <li>
          <button type="button" className={`co-link co-link--tab${activeTab === 'questions' ? ' is-tab' : ''}`} onClick={() => onNavigate('questions')}>
            <span className="co-num">3</span> Question bank <Count part={p.questions} />
          </button>
          <ol className="co-sub">
            {tierIds.map((t) => (
              <li key={t}>
                <button
                  type="button"
                  className={`co-link${activeTab === 'questions' && active === `tier-${t}` ? ' is-current' : ''}`}
                  aria-current={activeTab === 'questions' && active === `tier-${t}` ? 'location' : undefined}
                  onClick={() => onNavigate('questions', `tier-${t}`)}
                >
                  {TIER_LABEL[t]} <Count part={p.tiers[t]} />
                </button>
              </li>
            ))}
          </ol>
        </li>
      </ol>
    </nav>
  );
}

/**
 * Below 1200 px there is no gutter: the same outline opens as a sheet from a floating "Contents" button that also
 * shows the chapter percentage.
 */
export function ChapterOutlineSheet(props: Props) {
  const [open, setOpen] = useState(false);
  const percent = chapterProgress(props.chapter, useProgress().data).percent;

  useEffect(() => {
    if (!open) return;
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('keydown', esc);
    return () => document.removeEventListener('keydown', esc);
  }, [open]);

  return (
    <div className="co-sheet">
      <button type="button" className="ui-btn ui-btn--primary co-fab" aria-expanded={open} aria-controls="co-sheet-panel" onClick={() => setOpen((o) => !o)}>
        Contents · {percent}%
      </button>
      {open && (
        <>
          <div className="co-backdrop" onClick={() => setOpen(false)} aria-hidden="true" />
          <div className="co-sheet__panel" id="co-sheet-panel" role="dialog" aria-label="Chapter outline">
            <ChapterOutline
              {...props}
              onNavigate={(tab, id) => {
                setOpen(false);
                props.onNavigate(tab, id);
              }}
            />
          </div>
        </>
      )}
    </div>
  );
}
