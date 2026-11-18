import type {
  AnswerBody,
  AnswerResponse,
  CommitBody,
  CompletionResponse,
  DueCard,
  ProgressView,
  ReviewBody,
  ReviewResponse,
  TeaserResponse,
  TrapView,
} from '@/shared/api/types';

/**
 * Port for everything that changes the learner's state. Every write carries an attemptId, so callers may retry
 * freely. Implementations: RemoteProgressService (api, authoritative) and LocalProgressService (browser storage).
 */
export interface ProgressService {
  readonly mode: 'online' | 'offline';
  getProgress(): Promise<ProgressView>;
  submitAnswer(chapterId: string, questionId: string, body: AnswerBody): Promise<AnswerResponse>;
  commitTeaser(chapterId: string, teaserId: string, body: CommitBody): Promise<TeaserResponse>;
  completeChapter(chapterId: string, attemptId: string): Promise<CompletionResponse>;
  getTrapBoard(): Promise<TrapView[]>;
  getDueFlashcards(limit?: number): Promise<DueCard[]>;
  reviewFlashcard(body: ReviewBody): Promise<ReviewResponse>;
}
