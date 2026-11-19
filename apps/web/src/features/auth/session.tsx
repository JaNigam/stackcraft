import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { onSessionExpired } from '@/shared/api/http';
import { announceXp } from '@/features/progress/xpEvents';
import type { AuthService, Me } from './AuthService';
import type { LocalProgressService } from '@/features/progress/LocalProgressService';

export type SessionState = { status: 'loading' } | { status: 'guest' } | { status: 'signedIn'; me: Me };

export interface SessionApi {
  state: SessionState;
  available: boolean;
  /** Set after sign-in when guest progress was imported. */
  lastImport: { xp: number; events: number } | null;
  signIn(email: string, password: string): Promise<Me>;
  signUp(email: string, password: string, displayName: string): Promise<Me>;
  signOut(): Promise<void>;
  signOutEverywhere(): Promise<void>;
  updateProfile(update: { displayName?: string; leaderboardVisible?: boolean }): Promise<Me>;
}

const guestOnly: SessionApi = {
  state: { status: 'guest' },
  available: false,
  lastImport: null,
  signIn: () => Promise.reject(new Error('Accounts need the api')),
  signUp: () => Promise.reject(new Error('Accounts need the api')),
  signOut: async () => {},
  signOutEverywhere: async () => {},
  updateProfile: () => Promise.reject(new Error('Accounts need the api')),
};

const SessionContext = createContext<SessionApi>(guestOnly);

/** Cached data that belongs to a learner. Keys include who it belongs to; old entries are dropped on a switch to free memory. */
const PER_LEARNER_KEYS = [['progress'], ['trap-board'], ['flashcards']];

/**
 * Who is using the app right now. On start it asks the api (one silent refresh included); without an AuthService
 * (offline mode, static site) everyone is a guest. After sign-in, progress earned as a guest in this browser is
 * imported once and the local copy is cleared.
 */
export function SessionProvider({ auth, guestProgress, children }: { auth?: AuthService; guestProgress: LocalProgressService; children: ReactNode }) {
  const qc = useQueryClient();
  const [state, setState] = useState<SessionState>(auth ? { status: 'loading' } : { status: 'guest' });
  const [lastImport, setLastImport] = useState<SessionApi['lastImport']>(null);

  const switchTo = useCallback(
    (next: SessionState) => {
      setState(next);
      // Inactive entries only: active queries switch keys on the next render and fetch for the new learner.
      PER_LEARNER_KEYS.forEach((queryKey) => qc.removeQueries({ queryKey, type: 'inactive' }));
    },
    [qc],
  );

  useEffect(() => {
    if (!auth) return;
    let alive = true;
    auth
      .me()
      .then((me) => alive && switchTo(me ? { status: 'signedIn', me } : { status: 'guest' }))
      .catch(() => alive && switchTo({ status: 'guest' }));
    const off = onSessionExpired(() => switchTo({ status: 'guest' }));
    return () => {
      alive = false;
      off();
    };
  }, [auth, switchTo]);

  const api = useMemo<SessionApi>(() => {
    if (!auth) return guestOnly;
    const afterSignIn = async (me: Me) => {
      const payload = guestProgress.exportGuest();
      if (payload && payload.events.length + (payload.traps?.length ?? 0) + (payload.cards?.length ?? 0) > 0) {
        try {
          const r = await auth.importGuestProgress(payload);
          guestProgress.clearAfterImport();
          setLastImport({ xp: r.xpAwarded, events: r.eventsImported });
          announceXp(r.xpAwarded, 'imported from this browser');
        } catch {
          /* keep local progress; the next sign-in tries again (attempt ids make it safe) */
        }
      }
      switchTo({ status: 'signedIn', me });
      return me;
    };
    return {
      state,
      available: true,
      lastImport,
      signIn: async (email, password) => afterSignIn(await auth.login(email, password)),
      signUp: async (email, password, name) => afterSignIn(await auth.register(email, password, name)),
      signOut: async () => {
        await auth.logout().catch(() => {});
        setLastImport(null);
        switchTo({ status: 'guest' });
      },
      signOutEverywhere: async () => {
        await auth.logoutEverywhere();
        setLastImport(null);
        switchTo({ status: 'guest' });
      },
      updateProfile: async (update) => {
        const me = await auth.updateProfile(update);
        setState({ status: 'signedIn', me });
        return me;
      },
    };
  }, [auth, guestProgress, state, lastImport, switchTo]);

  return <SessionContext.Provider value={api}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionState {
  return useContext(SessionContext).state;
}

export function useSessionApi(): SessionApi {
  return useContext(SessionContext);
}
