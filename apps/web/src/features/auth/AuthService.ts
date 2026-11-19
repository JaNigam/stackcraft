import type { GuestImport, ImportResult, Me } from '@/shared/api/types';

export type { GuestImport, ImportResult, Me };

/** Port for accounts and sessions. The only adapter today is the api; tests use fakes. */
export interface AuthService {
  /** The signed-in learner, or null for a guest (after one silent refresh attempt). */
  me(): Promise<Me | null>;
  register(email: string, password: string, displayName: string): Promise<Me>;
  login(email: string, password: string): Promise<Me>;
  logout(): Promise<void>;
  logoutEverywhere(): Promise<void>;
  updateProfile(update: { displayName?: string; leaderboardVisible?: boolean }): Promise<Me>;
  importGuestProgress(payload: GuestImport): Promise<ImportResult>;
}
