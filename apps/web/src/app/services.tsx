import { createContext, useContext, type ReactNode } from 'react';
import type { ContentService } from '@/entities/chapter/ContentService';
import { RemoteContentService } from '@/entities/chapter/RemoteContentService';
import { BundledContentService } from '@/entities/chapter/BundledContentService';
import type { ProgressService } from '@/features/progress/ProgressService';
import { RemoteProgressService } from '@/features/progress/RemoteProgressService';
import { LocalProgressService } from '@/features/progress/LocalProgressService';
import type { CodeRunner } from '@/features/code-runner/CodeRunner';
import { OfflineCodeRunner, RemoteCodeRunner } from '@/features/code-runner/RemoteCodeRunner';
import type { AuthService } from '@/features/auth/AuthService';
import { RemoteAuthService } from '@/features/auth/RemoteAuthService';
import { useSession } from '@/features/auth/session';

/**
 * Composition root of the UI. Features ask for interfaces through these hooks and never know which adapter they
 * got (DIP). Tests pass fakes through <ServicesProvider value={...}>.
 *
 * Progress has two adapters at once when the api is up: `guestProgress` (this browser) and `accountProgress` (the
 * api, for a signed-in learner). useProgressService picks one from the current session (ADR 0004).
 */
export interface Services {
  mode: 'online' | 'offline';
  content: ContentService;
  guestProgress: LocalProgressService;
  accountProgress?: ProgressService;
  runner: CodeRunner;
  /** Absent in offline mode and on the static site: no accounts there. */
  auth?: AuthService;
}

const ServicesContext = createContext<Services | null>(null);

export function ServicesProvider({ value, children }: { value: Services; children: ReactNode }) {
  return <ServicesContext.Provider value={value}>{children}</ServicesContext.Provider>;
}

export function useServices(): Services {
  const s = useContext(ServicesContext);
  if (!s) throw new Error('ServicesProvider missing');
  return s;
}

export const useContent = () => useServices().content;
export const useCodeRunner = () => useServices().runner;
export const useMode = () => useServices().mode;
export const useAuthService = () => useServices().auth;

/** Signed in: the api. Guest (or still checking): this browser. */
export function useProgressService(): ProgressService {
  const s = useServices();
  const session = useSession();
  return session.status === 'signedIn' && s.accountProgress ? s.accountProgress : s.guestProgress;
}

export function onlineServices(): Services {
  return {
    mode: 'online',
    content: new RemoteContentService(),
    guestProgress: new LocalProgressService(),
    accountProgress: new RemoteProgressService(),
    runner: new RemoteCodeRunner(),
    auth: new RemoteAuthService(),
  };
}

export function offlineServices(): Services {
  return { mode: 'offline', content: new BundledContentService(), guestProgress: new LocalProgressService(), runner: new OfflineCodeRunner() };
}

/**
 * Picks the adapters once at start-up: if the api answers within 1.5 s we go online, otherwise the whole app runs
 * from the bundle and browser storage. `?offline` forces offline mode.
 */
export async function chooseServices(): Promise<Services> {
  // Static hosting (GitHub Pages) has no api: skip the probe and go straight to offline mode.
  if (import.meta.env.VITE_STATIC_SITE === 'true') return offlineServices();
  if (new URLSearchParams(location.search).has('offline')) return offlineServices();
  try {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 1500);
    const res = await fetch('/api/v1/series', { signal: ctrl.signal, headers: { Accept: 'application/json' }, credentials: 'include' });
    clearTimeout(timer);
    if (res.ok && (res.headers.get('content-type') ?? '').includes('json')) return onlineServices();
  } catch {
    /* api unreachable */
  }
  return offlineServices();
}
