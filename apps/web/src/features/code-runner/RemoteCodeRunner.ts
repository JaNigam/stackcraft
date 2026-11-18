import { api } from '@/shared/api/http';
import type { CodeRunner, RunOutcome } from './CodeRunner';

export class RemoteCodeRunner implements CodeRunner {
  async run(source: string, stdin?: string): Promise<RunOutcome> {
    try {
      const { data, error, response } = await api.POST('/api/v1/runs', { body: { source, stdin } });
      if (data && response.ok) return { kind: 'ran', result: data };
      const detail = (error as { detail?: string } | undefined)?.detail ?? `HTTP ${response.status}`;
      if (response.status === 429) return { kind: 'busy', message: detail };
      return { kind: 'unavailable', message: detail };
    } catch (e) {
      return { kind: 'unavailable', message: e instanceof Error ? e.message : 'Network error' };
    }
  }
}

export class OfflineCodeRunner implements CodeRunner {
  async run(): Promise<RunOutcome> {
    return { kind: 'unavailable', message: 'Offline mode: no code runner.' };
  }
}
