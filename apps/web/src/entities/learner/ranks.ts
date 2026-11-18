import type { ProgressView } from '@/shared/api/types';

/** Course ranks. Only the offline service needs these; online, the api sends rank and thresholds. */
export const RANKS = [
  { id: 'TRAINEE', label: 'Trainee', minXp: 0 },
  { id: 'DEVELOPER', label: 'Developer', minXp: 200 },
  { id: 'SENIOR', label: 'Senior', minXp: 500 },
  { id: 'STAFF', label: 'Staff', minXp: 900 },
  { id: 'PRINCIPAL', label: 'Principal', minXp: 1300 },
] as const;

export function rankFor(xp: number) {
  let index = 0;
  RANKS.forEach((r, i) => {
    if (xp >= r.minXp) index = i;
  });
  return { rank: RANKS[index], next: RANKS[index + 1] };
}

/** 0..1 progress through the current rank band, for the XP bar. */
export function rankProgress(p: Pick<ProgressView, 'totalXp' | 'rankMinXp' | 'nextRankMinXp'>): number {
  if (p.nextRankMinXp === null || p.nextRankMinXp === undefined) return 1;
  return Math.min(1, Math.max(0, (p.totalXp - p.rankMinXp) / (p.nextRankMinXp - p.rankMinXp)));
}
