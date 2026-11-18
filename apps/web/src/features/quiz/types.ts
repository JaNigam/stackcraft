import type { AnswerBody, AnswerResponse, Question } from '@/shared/api/types';

/** The one contract every question renderer implements (LSP): any renderer can stand in for any other. */
export interface QuestionRendererProps {
  question: Question;
  /** attemptId is added by the container; renderers only say what the learner answered. */
  onSubmit: (answer: Omit<AnswerBody, 'attemptId'>) => Promise<AnswerResponse>;
  /** Best score so far for this question (0..1), if attempted before. */
  best?: number;
}
