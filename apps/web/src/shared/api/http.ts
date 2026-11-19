import createClient from 'openapi-fetch';
import type { paths } from './schema';

/**
 * Every api call goes through authFetch (ADR 0004):
 * - cookies are sent (`credentials: 'include'`): the session lives in HttpOnly cookies JS cannot read;
 * - writes carry `X-XSRF-TOKEN`, echoing the readable `XSRF-TOKEN` cookie (CSRF protection);
 * - a 401 triggers ONE refresh (serialised across tabs with the Web Locks API, so two tabs never present the same
 *   refresh token and trip reuse detection), then the request is retried once.
 */
const AUTH_PATH = '/api/v1/auth/';
const sessionListeners = new Set<() => void>();

export function onSessionExpired(l: () => void): () => void {
  sessionListeners.add(l);
  return () => sessionListeners.delete(l);
}

export function csrfToken(cookie: string = typeof document === 'undefined' ? '' : document.cookie): string | undefined {
  const m = cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : undefined;
}

type BaseFetch = (input: Request) => Promise<Response>;

export function makeAuthFetch(base: BaseFetch, readCsrf: () => string | undefined = () => csrfToken()) {
  let refreshing: Promise<boolean> | null = null;

  const prepare = (req: Request): Request => {
    const r = new Request(req, { credentials: 'include' });
    const token = readCsrf();
    if (r.method !== 'GET' && r.method !== 'HEAD' && token) r.headers.set('X-XSRF-TOKEN', token);
    return r;
  };

  const refreshOnce = (origin: string): Promise<boolean> => {
    refreshing ??= (async () => {
      const run = async () => (await base(prepare(new Request(`${origin}${AUTH_PATH}refresh`, { method: 'POST' })))).ok;
      try {
        const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
        return locks ? await locks.request('stackcraft-refresh', run) : await run();
      } catch {
        return false;
      } finally {
        queueMicrotask(() => (refreshing = null));
      }
    })();
    return refreshing;
  };

  return async function authFetch(input: Request): Promise<Response> {
    const retry = input.clone();
    const res = await base(prepare(input));
    if (res.status !== 401 || new URL(input.url).pathname.startsWith(AUTH_PATH)) return res;
    if (await refreshOnce(new URL(input.url).origin)) return base(prepare(retry));
    sessionListeners.forEach((l) => l());
    return res;
  };
}

export const authFetch = makeAuthFetch((r) => fetch(r));

/** Typed client generated from docs/api/openapi.yaml. Same origin: dev proxy (Vite) or nginx forward /api. */
export const api = createClient<paths>({ baseUrl: '', fetch: authFetch });

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problemType: string | undefined,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** Unwraps an openapi-fetch result, turning RFC 9457 problem bodies into ApiError. */
export function unwrap<T>(result: { data?: T; error?: unknown; response: Response }): T {
  if (result.data !== undefined && result.response.ok) return result.data;
  const problem = (result.error ?? {}) as { type?: string; detail?: string; title?: string };
  throw new ApiError(result.response.status, problem.type, problem.detail ?? problem.title ?? `HTTP ${result.response.status}`);
}

export function newAttemptId(): string {
  return crypto.randomUUID();
}
