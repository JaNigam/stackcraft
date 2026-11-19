import type { ChapterView, ProgressView } from '@/shared/api/types';

/**
 * How far a learner is through one chapter, derived only from data the app already has: the chapter (items and
 * their XP) and the learner's progress (best score per item, completed chapters). Pure, so it works the same for a
 * guest (browser storage) and a signed-in learner (api).
 */
export interface PartProgress {
  done: number;
  total: number;
}

export interface ChapterProgress {
  lessonComplete: boolean;
  teasers: PartProgress;
  questions: PartProgress;
  /** By tier, for the question bank sub-sections. */
  tiers: Record<'warmup' | 'interview' | 'killer', PartProgress>;
  xpEarned: number;
  xpAvailable: number;
  /** 0..100, weighted by XP so a killer problem counts more than a warm-up. */
  percent: number;
}

export function chapterProgress(chapter: ChapterView, progress: ProgressView | undefined): ChapterProgress {
  const id = chapter.meta.id;
  const best = (itemId: string) => progress?.bestScores[`${id}/${itemId}`] ?? 0;
  const lessonComplete = progress?.completedChapters.includes(id) ?? false;

  const tiers: ChapterProgress['tiers'] = {
    warmup: { done: 0, total: 0 },
    interview: { done: 0, total: 0 },
    killer: { done: 0, total: 0 },
  };
  let xpEarned = lessonComplete ? chapter.meta.completionXp : 0;
  let xpAvailable = chapter.meta.completionXp;

  for (const q of chapter.questions) {
    const s = best(q.id);
    tiers[q.tier].total++;
    if (s >= 1) tiers[q.tier].done++;
    xpEarned += Math.round(q.xp * s);
    xpAvailable += q.xp;
  }

  let teasersDone = 0;
  for (const t of chapter.teasers) {
    const s = best(t.id);
    if (s >= 1) teasersDone++;
    xpEarned += Math.round(t.xp * s);
    xpAvailable += t.xp;
  }

  const questionsDone = tiers.warmup.done + tiers.interview.done + tiers.killer.done;
  return {
    lessonComplete,
    teasers: { done: teasersDone, total: chapter.teasers.length },
    questions: { done: questionsDone, total: chapter.questions.length },
    tiers,
    xpEarned,
    xpAvailable,
    percent: xpAvailable === 0 ? 0 : Math.round((Math.min(xpEarned, xpAvailable) / xpAvailable) * 100),
  };
}
