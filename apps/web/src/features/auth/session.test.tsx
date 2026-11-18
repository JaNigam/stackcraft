import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ServicesProvider, offlineServices, useProgressService, type Services } from '@/app/services';
import { LocalProgressService } from '@/features/progress/LocalProgressService';
import type { ProgressService } from '@/features/progress/ProgressService';
import type { AuthService, Me } from './AuthService';
import { SessionProvider, useSessionApi } from './session';
import { validate } from './AuthPage';

const ana: Me = { id: '11111111-1111-1111-1111-111111111111', email: 'ana@example.com', displayName: 'Ana', leaderboardVisible: true };

function fakeAuth(initial: Me | null): AuthService & { imports: unknown[] } {
  const imports: unknown[] = [];
  return {
    imports,
    me: vi.fn(async () => initial),
    login: vi.fn(async () => ana),
    register: vi.fn(async () => ana),
    logout: vi.fn(async () => {}),
    logoutEverywhere: vi.fn(async () => {}),
    updateProfile: vi.fn(async (u) => ({ ...ana, ...u })),
    importGuestProgress: vi.fn(async (p) => {
      imports.push(p);
      return { eventsImported: p.events.length, eventsSkipped: 0, xpAwarded: 2, progress: {} as never };
    }),
  };
}

let api: ReturnType<typeof useSessionApi>;
let progress: ProgressService;

function Probe() {
  api = useSessionApi();
  progress = useProgressService();
  return <span data-testid="state">{api.state.status === 'signedIn' ? `in:${api.state.me.displayName}` : api.state.status}</span>;
}

function setup(auth: AuthService) {
  const guest = new LocalProgressService();
  const account = {} as ProgressService;
  const services: Services = { ...offlineServices(), guestProgress: guest, accountProgress: account, auth };
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ServicesProvider value={services}>
        <SessionProvider auth={auth} guestProgress={guest}>
          <Probe />
        </SessionProvider>
      </ServicesProvider>
    </QueryClientProvider>,
  );
  return { guest, account };
}

describe('SessionProvider', () => {
  it('starts as a guest using browser progress, then switches to the account on sign-in', async () => {
    const auth = fakeAuth(null);
    const { guest, account } = setup(auth);
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('guest'));
    expect(progress).toBe(guest);

    await act(() => api.signIn('ana@example.com', 'correct horse battery'));

    expect(screen.getByTestId('state')).toHaveTextContent('in:Ana');
    expect(progress).toBe(account);
  });

  it('imports guest progress once after sign-in and clears the browser copy', async () => {
    const auth = fakeAuth(null);
    const { guest } = setup(auth);
    await guest.submitAnswer('java-core.u00.l01', 'w1', { attemptId: 'attempt-0001', choice: 3 });
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('guest'));

    await act(() => api.signUp('ana@example.com', 'correct horse battery', 'Ana'));

    expect(auth.importGuestProgress).toHaveBeenCalledOnce();
    expect(JSON.stringify(auth.imports[0])).toContain('attempt-0001');
    expect(JSON.stringify(auth.imports[0])).not.toContain('"xp"');
    expect(guest.exportGuest()).toBeNull();
    expect(api.lastImport).toEqual({ xp: 2, events: 1 });

    await act(() => api.signOut());
    await act(() => api.signIn('ana@example.com', 'correct horse battery'));
    expect(auth.importGuestProgress).toHaveBeenCalledOnce(); // nothing left to import
  });

  it('restores a signed-in session on load, and logout returns to guest', async () => {
    const auth = fakeAuth(ana);
    setup(auth);
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('in:Ana'));

    await act(() => api.signOut());

    expect(auth.logout).toHaveBeenCalledOnce();
    expect(screen.getByTestId('state')).toHaveTextContent('guest');
  });
});

describe('sign-in form validation', () => {
  it('mirrors the server rules', () => {
    expect(validate('signin', 'nope', 'x', '')).toMatch(/valid email/);
    expect(validate('signin', 'a@b.co', '', '')).toMatch(/password/);
    expect(validate('signup', 'a@b.co', 'short', 'Ana')).toMatch(/10 characters/);
    expect(validate('signup', 'ana@example.com', 'ana@example.com', 'Ana')).toMatch(/not be your email/);
    expect(validate('signup', 'a@b.co', 'correct horse battery', 'A')).toMatch(/2 to 32/);
    expect(validate('signup', 'a@b.co', 'correct horse battery', 'Ana')).toBeNull();
  });
});
