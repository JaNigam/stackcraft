import { describe, expect, it } from 'vitest';
import type { Question, QuestionType } from '@/shared/api/types';
import { grade, graders, normaliseOutput } from './graders';

const samples: Record<QuestionType, { q: Question; right: Parameters<typeof grade>[1] }> = {
  mcq: { q: { id: 'm', type: 'mcq', tier: 'warmup', prompt: 'p', options: ['a', 'b'], answer: 1, explanation: 'e', xp: 2 }, right: { attemptId: 'x', choice: 1 } },
  predict: { q: { id: 'p', type: 'predict', tier: 'interview', prompt: 'p', code: 'c', expected: '33\n', explanation: 'e', xp: 4 }, right: { attemptId: 'x', text: '33' } },
  short: { q: { id: 's', type: 'short', tier: 'interview', prompt: 'p', modelAnswer: 'm', keyPoints: ['k'], xp: 4 }, right: { attemptId: 'x', selfScore: 2 } },
  problem: { q: { id: 'k', type: 'problem', tier: 'killer', prompt: 'p', hints: ['1', '2', '3'], modelAnswer: 'm', xp: 8 }, right: { attemptId: 'x', selfScore: 2, hintsUsed: 0 } },
};

// Same contract as the api's GraderContractTest.
describe.each(Object.keys(graders) as QuestionType[])('%s grader contract', (type) => {
  const { q, right } = samples[type];

  it('never throws and keeps score in [0, 1] for hostile input', () => {
    for (const a of [{ attemptId: 'x' }, { attemptId: 'x', choice: -1, text: '', selfScore: -5, hintsUsed: -3 }, { attemptId: 'x', choice: 99, selfScore: 99, hintsUsed: 99 }]) {
      const g = grade(q, a);
      expect(g.score).toBeGreaterThanOrEqual(0);
      expect(g.score).toBeLessThanOrEqual(1);
    }
  });

  it('gives full marks for the right answer and zero for an empty one', () => {
    expect(grade(q, right)).toMatchObject({ correct: true, score: 1 });
    expect(grade(q, { attemptId: 'x' })).toMatchObject({ correct: false, score: 0 });
  });
});

describe('specific rules', () => {
  it('normalises output like the server', () => {
    expect(normaliseOutput('  [a, b]  \r\n{x=1}   \r\n\r\n')).toBe('[a, b]\n{x=1}');
  });

  it('charges 20% per hint with a 40% floor', () => {
    const q = samples.problem.q;
    expect(grade(q, { attemptId: 'x', selfScore: 2, hintsUsed: 1 }).score).toBeCloseTo(0.8);
    expect(grade(q, { attemptId: 'x', selfScore: 2, hintsUsed: 3 }).score).toBeCloseTo(0.4);
  });
});
