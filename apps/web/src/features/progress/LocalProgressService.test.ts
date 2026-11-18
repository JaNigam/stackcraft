import { describe, expect, it } from 'vitest';
import { LocalProgressService } from './LocalProgressService';

const CH = 'java-core.u00.l01';
const fixedNow = () => new Date('2026-11-19T10:00:00');

function svc() {
  return new LocalProgressService(localStorage, fixedNow);
}

describe('LocalProgressService (offline rules match the api)', () => {
  it('awards once per attemptId and never lets an item be farmed', async () => {
    const s = svc();
    const first = await s.submitAnswer(CH, 'w1', { attemptId: 'attempt-0001', choice: 3 });
    expect(first).toMatchObject({ correct: true, xpAwarded: 2, duplicate: false });

    const retry = await s.submitAnswer(CH, 'w1', { attemptId: 'attempt-0001', choice: 3 });
    expect(retry).toMatchObject({ duplicate: true });
    expect(retry.progress.totalXp).toBe(2);

    const again = await s.submitAnswer(CH, 'w1', { attemptId: 'attempt-0002', choice: 3 });
    expect(again.xpAwarded).toBe(0);
    expect((await s.getProgress()).totalXp).toBe(2);
  });

  it('puts wrong answers on the Trap Board and clears them when answered right', async () => {
    const s = svc();
    await s.commitTeaser(CH, 't2', { attemptId: 'attempt-0003', choice: 1 }); // guessed 25, answer is 10
    await s.submitAnswer(CH, 'i2', { attemptId: 'attempt-0004', text: 'null\n2\n1 2 null' });
    expect((await s.getTrapBoard()).map((t) => t.itemId).sort()).toEqual(['i2', 't2']);

    const fixed = await s.submitAnswer(CH, 'i2', { attemptId: 'attempt-0005', text: 'null\n1\n1 2 null\n' });
    expect(fixed.correct).toBe(true);
    expect((await s.getTrapBoard()).map((t) => t.itemId)).toEqual(['t2']);
  });

  it('unlocks flashcards on completion and moves them up a Leitner box', async () => {
    const s = svc();
    expect(await s.getDueFlashcards()).toHaveLength(0);
    const done = await s.completeChapter(CH, 'attempt-0006');
    expect(done.xpAwarded).toBe(10);
    expect(done.progress.completedChapters).toEqual([CH]);

    const due = await s.getDueFlashcards(10);
    expect(due.length).toBeGreaterThan(0);
    const r = await s.reviewFlashcard({ attemptId: 'attempt-0007', cardKey: due[0].key, remembered: true });
    expect(r).toMatchObject({ box: 2, dueOn: '2026-11-21', xpAwarded: 1 });
    expect((await s.getDueFlashcards(10)).map((c) => c.key)).not.toContain(due[0].key);
  });

  it('reports rank, next rank and a streak', async () => {
    const p = await svc().getProgress();
    expect(p).toMatchObject({ totalXp: 0, rank: 'TRAINEE', nextRank: 'DEVELOPER', xpToNextRank: 200, streakDays: 0 });
  });

  it('rejects unsafe attempt ids like the api', async () => {
    await expect(svc().submitAnswer(CH, 'w1', { attemptId: 'x', choice: 3 })).rejects.toThrow(/attemptId/);
  });
});
