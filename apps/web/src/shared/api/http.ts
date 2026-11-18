import createClient from 'openapi-fetch';
import type { paths } from './schema';

/** Typed client generated from docs/api/openapi.yaml. Same origin: dev proxy (Vite) or nginx forward /api. */
export const api = createClient<paths>({ baseUrl: '' });

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
