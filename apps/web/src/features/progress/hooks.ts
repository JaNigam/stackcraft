import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useProgressService } from '@/app/services';
import type { AnswerBody, CommitBody, ProgressView, ReviewBody } from '@/shared/api/types';
import { announceXp } from './xpEvents';

/**
 * Small focused hooks (ISP) over one cached progress query. Mutations keep the same attemptId across their own
 * retries, so a flaky network can never double-award XP: the server answers `duplicate: true`.
 */
const KEY = ['progress'];

export function useProgress() {
  const svc = useProgressService();
  return useQuery({ queryKey: KEY, queryFn: () => svc.getProgress() });
}

export function useXp() {
  const { data } = useProgress();
  return data ? { total: data.totalXp, rank: data.rankLabel, next: data.nextRankLabel, toNext: data.xpToNextRank, progress: data } : undefined;
}

export function useStreak() {
  return useProgress().data?.streakDays ?? 0;
}

export function useCompleted(chapterId: string) {
  return useProgress().data?.completedChapters.includes(chapterId) ?? false;
}

export function useTrapBoard() {
  const svc = useProgressService();
  return useQuery({ queryKey: ['trap-board'], queryFn: () => svc.getTrapBoard() });
}

export function useDueFlashcards(limit = 5) {
  const svc = useProgressService();
  return useQuery({ queryKey: ['flashcards', 'due', limit], queryFn: () => svc.getDueFlashcards(limit) });
}

function useAfterWrite() {
  const qc = useQueryClient();
  return (r: { progress: ProgressView; xpAwarded: number; duplicate: boolean }, label: string) => {
    qc.setQueryData(KEY, r.progress);
    qc.invalidateQueries({ queryKey: ['trap-board'] });
    if (!r.duplicate) announceXp(r.xpAwarded, label);
  };
}

export function useSubmitAnswer(chapterId: string, questionId: string) {
  const svc = useProgressService();
  const after = useAfterWrite();
  return useMutation({
    mutationFn: (body: AnswerBody) => svc.submitAnswer(chapterId, questionId, body),
    retry: 2,
    onSuccess: (r) => after(r, 'answer'),
  });
}

export function useCommitTeaser(chapterId: string, teaserId: string) {
  const svc = useProgressService();
  const after = useAfterWrite();
  return useMutation({
    mutationFn: (body: CommitBody) => svc.commitTeaser(chapterId, teaserId, body),
    retry: 2,
    onSuccess: (r) => after(r, 'teaser'),
  });
}

export function useCompleteChapter(chapterId: string) {
  const svc = useProgressService();
  const after = useAfterWrite();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (attemptId: string) => svc.completeChapter(chapterId, attemptId),
    retry: 2,
    onSuccess: (r) => {
      after(r, 'lesson complete');
      qc.invalidateQueries({ queryKey: ['flashcards'] });
    },
  });
}

export function useReviewFlashcard() {
  const svc = useProgressService();
  const after = useAfterWrite();
  return useMutation({
    mutationFn: (body: ReviewBody) => svc.reviewFlashcard(body),
    retry: 2,
    onSuccess: (r) => after(r, 'flashcard'),
  });
}
