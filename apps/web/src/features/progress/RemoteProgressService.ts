import { api, unwrap } from '@/shared/api/http';
import type { AnswerBody, CommitBody, ReviewBody } from '@/shared/api/types';
import type { ProgressService } from './ProgressService';

export class RemoteProgressService implements ProgressService {
  readonly mode = 'online' as const;

  async getProgress() {
    return unwrap(await api.GET('/api/v1/progress'));
  }

  async submitAnswer(chapterId: string, questionId: string, body: AnswerBody) {
    return unwrap(
      await api.POST('/api/v1/chapters/{chapterId}/questions/{questionId}/answers', {
        params: { path: { chapterId, questionId } },
        body,
      }),
    );
  }

  async commitTeaser(chapterId: string, teaserId: string, body: CommitBody) {
    return unwrap(
      await api.POST('/api/v1/chapters/{chapterId}/teasers/{teaserId}/commits', { params: { path: { chapterId, teaserId } }, body }),
    );
  }

  async completeChapter(chapterId: string, attemptId: string) {
    return unwrap(await api.POST('/api/v1/chapters/{chapterId}/completion', { params: { path: { chapterId } }, body: { attemptId } }));
  }

  async getTrapBoard() {
    return unwrap(await api.GET('/api/v1/trap-board'));
  }

  async getDueFlashcards(limit = 5) {
    return unwrap(await api.GET('/api/v1/flashcards/due', { params: { query: { limit } } }));
  }

  async reviewFlashcard(body: ReviewBody) {
    return unwrap(await api.POST('/api/v1/flashcards/reviews', { body }));
  }
}
