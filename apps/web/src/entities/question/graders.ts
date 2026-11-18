import type { AnswerBody, Question, QuestionType } from '@/shared/api/types';

/**
 * Client-side mirror of the api's graders, used only by the offline LocalProgressService. Same rules, same
 * contract: never throw, score in [0, 1], pure. The api stays the authority when it is reachable.
 */
export interface Grade {
  correct: boolean;
  score: number;
  feedback: string;
  explanation: string;
}

type Grader = (q: Question, a: AnswerBody) => Grade;

/** Same rule as PredictOutputGrader.normalise on the server. */
export function normaliseOutput(s: string): string {
  return s
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((l) => l.replace(/\s+$/, ''))
    .join('\n')
    .trim();
}

const clampSelf = (n: number | undefined) => (n === undefined || n === null ? 0 : Math.max(0, Math.min(2, Math.trunc(n))));
const selfFeedback = (s: number) => (s === 2 ? 'Nailed it.' : s === 1 ? 'Partly there: re-read the key points.' : 'Missed it: this one goes on your Trap Board.');

export const graders: Record<QuestionType, Grader> = {
  mcq: (q, a) => {
    const correct = a.choice !== undefined && a.choice === q.answer;
    const right = q.options?.[q.answer ?? -1] ?? '';
    return { correct, score: correct ? 1 : 0, feedback: correct ? 'Correct.' : `Not quite. The answer is: ${right}`, explanation: q.explanation ?? '' };
  },
  predict: (q, a) => {
    const correct = typeof a.text === 'string' && normaliseOutput(a.text) === normaliseOutput(q.expected ?? '');
    return { correct, score: correct ? 1 : 0, feedback: correct ? 'Exactly right.' : `The program actually prints:\n${(q.expected ?? '').trim()}`, explanation: q.explanation ?? '' };
  },
  short: (q, a) => {
    const s = clampSelf(a.selfScore);
    return { correct: s === 2, score: s / 2, feedback: selfFeedback(s), explanation: q.modelAnswer ?? '' };
  },
  problem: (q, a) => {
    const s = clampSelf(a.selfScore);
    const hints = Math.max(0, Math.min(a.hintsUsed ?? 0, q.hints?.length ?? 0));
    const factor = Math.max(0.4, 1 - 0.2 * hints);
    const suffix = hints > 0 ? ` (${hints} hint${hints > 1 ? 's' : ''} used)` : '';
    return { correct: s === 2, score: (s / 2) * factor, feedback: selfFeedback(s) + suffix, explanation: q.modelAnswer ?? '' };
  },
};

export function grade(q: Question, a: AnswerBody): Grade {
  return graders[q.type](q, a);
}
