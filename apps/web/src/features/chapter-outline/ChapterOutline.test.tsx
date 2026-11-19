import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { bundledContent } from '@/entities/chapter/bundledContent';
import type { ProgressView } from '@/shared/api/types';
import { renderWithServices } from '@/test/renderWithServices';
import { slugify } from '@/shared/lib/slugify';
import { chapterProgress } from './chapterProgress';
import { ChapterOutline } from './ChapterOutline';

const chapter = bundledContent().chapters.get('java-core.u00.l01')!.view;
const CH = chapter.meta.id;

function progress(best: Record<string, number>, completed = false): ProgressView {
  return {
    learnerId: 'x', totalXp: 0, rank: 'TRAINEE', rankLabel: 'Trainee', rankMinXp: 0, nextRank: 'DEVELOPER', nextRankLabel: 'Developer',
    nextRankMinXp: 200, xpToNextRank: 200, streakDays: 0, completedChapters: completed ? [CH] : [],
    bestScores: Object.fromEntries(Object.entries(best).map(([k, v]) => [`${CH}/${k}`, v])),
  };
}

describe('chapterProgress', () => {
  it('is zero with no progress, and the XP available is the whole chapter', () => {
    const p = chapterProgress(chapter, undefined);
    const questionXp = chapter.questions.reduce((n, q) => n + q.xp, 0);
    const teaserXp = chapter.teasers.reduce((n, t) => n + t.xp, 0);
    expect(p).toMatchObject({ percent: 0, xpEarned: 0, xpAvailable: chapter.meta.completionXp + questionXp + teaserXp, lessonComplete: false });
    expect(p.questions).toEqual({ done: 0, total: chapter.questions.length });
    expect(p.tiers.warmup.total).toBe(5);
  });

  it('counts only full marks as done but gives partial XP, weighted by XP', () => {
    const p = chapterProgress(chapter, progress({ w1: 1, w2: 1, k1: 0.5, t1: 1 }, true));
    expect(p.tiers.warmup.done).toBe(2);
    expect(p.tiers.killer.done).toBe(0);
    expect(p.teasers.done).toBe(1);
    expect(p.xpEarned).toBe(10 + 2 + 2 + 4 + 1); // lesson + two warm-ups + half a killer + one teaser
    expect(p.percent).toBe(Math.round((19 / p.xpAvailable) * 100));
  });

  it('slugs are stable and URL safe', () => {
    expect(slugify('How a HashSet picks the bucket (exact internals)')).toBe('how-a-hashset-picks-the-bucket-exact-internals');
    expect(slugify('Choosing, in 10 seconds')).toBe('choosing-in-10-seconds');
  });
});

describe('ChapterOutline', () => {
  it('lists sections found on the page and navigates on click', async () => {
    render(
      <div className="th">
        <h2 id="the-family-tree" data-outline="The family tree" />
        <section id="cheat-card" data-outline="Cheat card" />
      </div>,
    );
    const onNavigate = vi.fn();
    renderWithServices(<ChapterOutline chapter={chapter} activeTab="theory" onNavigate={onNavigate} />);

    await waitFor(() => expect(screen.getByRole('button', { name: 'The family tree' })).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: 'Cheat card' }));
    expect(onNavigate).toHaveBeenCalledWith('theory', 'cheat-card');

    await userEvent.click(screen.getByRole('button', { name: /Killer/ }));
    expect(onNavigate).toHaveBeenCalledWith('questions', 'tier-killer');

    await userEvent.click(screen.getByRole('button', { name: /Brain teasers/ }));
    expect(onNavigate).toHaveBeenCalledWith('teasers');
    expect(screen.getByRole('progressbar', { name: 'Chapter progress' })).toHaveAttribute('aria-valuenow', '0');
  });
});
