import { describe, expect, it, vi } from 'vitest';
import { csrfToken, makeAuthFetch, onSessionExpired } from './http';

const URL_BASE = 'http://localhost:5173';

function fakeBase(responses: Record<string, number[]>) {
  const calls: Request[] = [];
  const base = vi.fn(async (req: Request) => {
    calls.push(req);
    const path = new URL(req.url).pathname;
    const queue = responses[path] ?? [200];
    const status = queue.length > 1 ? queue.shift()! : queue[0];
    return new Response(status === 204 ? null : '{}', { status, headers: { 'Content-Type': 'application/json' } });
  });
  return { base, calls };
}

describe('authFetch', () => {
  it('sends cookies everywhere and the CSRF header on writes only', async () => {
    const { base, calls } = fakeBase({});
    const f = makeAuthFetch(base, () => 'tok-123');

    await f(new Request(`${URL_BASE}/api/v1/series`));
    await f(new Request(`${URL_BASE}/api/v1/progress/imports`, { method: 'POST', body: '{}' }));

    expect(calls[0].credentials).toBe('include');
    expect(calls[0].headers.get('X-XSRF-TOKEN')).toBeNull();
    expect(calls[1].headers.get('X-XSRF-TOKEN')).toBe('tok-123');
  });

  it('on 401 refreshes once and retries the original request with its body', async () => {
    const { base, calls } = fakeBase({ '/api/v1/progress/imports': [401, 200], '/api/v1/auth/refresh': [200] });
    const f = makeAuthFetch(base, () => 'tok');

    const res = await f(new Request(`${URL_BASE}/api/v1/progress/imports`, { method: 'POST', body: '{"events":[]}' }));

    expect(res.status).toBe(200);
    expect(calls.map((c) => new URL(c.url).pathname)).toEqual(['/api/v1/progress/imports', '/api/v1/auth/refresh', '/api/v1/progress/imports']);
    expect(await calls[2].text()).toBe('{"events":[]}');
  });

  it('concurrent 401s share a single refresh call', async () => {
    const { base, calls } = fakeBase({ '/api/v1/progress': [401, 401, 200], '/api/v1/trap-board': [401, 200], '/api/v1/auth/refresh': [200] });
    const f = makeAuthFetch(base, () => 'tok');

    await Promise.all([f(new Request(`${URL_BASE}/api/v1/progress`)), f(new Request(`${URL_BASE}/api/v1/trap-board`))]);

    expect(calls.filter((c) => c.url.endsWith('/auth/refresh'))).toHaveLength(1);
  });

  it('a failed refresh signals "session expired" and returns the 401', async () => {
    const { base } = fakeBase({ '/api/v1/progress': [401], '/api/v1/auth/refresh': [401] });
    const f = makeAuthFetch(base, () => 'tok');
    const expired = vi.fn();
    const off = onSessionExpired(expired);

    const res = await f(new Request(`${URL_BASE}/api/v1/progress`));

    expect(res.status).toBe(401);
    expect(expired).toHaveBeenCalledOnce();
    off();
  });

  it('never tries to refresh for the auth endpoints themselves', async () => {
    const { base, calls } = fakeBase({ '/api/v1/auth/login': [401] });
    const f = makeAuthFetch(base, () => 'tok');

    await f(new Request(`${URL_BASE}/api/v1/auth/login`, { method: 'POST', body: '{}' }));

    expect(calls).toHaveLength(1);
  });

  it('reads the XSRF-TOKEN cookie', () => {
    expect(csrfToken('a=1; XSRF-TOKEN=abc%2Ddef; b=2')).toBe('abc-def');
    expect(csrfToken('a=1')).toBeUndefined();
  });
});
