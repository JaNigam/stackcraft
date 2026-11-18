import type { RunResult } from '@/shared/api/types';

/** What happened when we asked to run code. "unavailable" and "busy" are normal states, not exceptions. */
export type RunOutcome =
  | { kind: 'ran'; result: RunResult }
  | { kind: 'busy'; message: string }
  | { kind: 'unavailable'; message: string };

/** Port: run a Java snippet. RemoteCodeRunner goes through the api; OfflineCodeRunner always says unavailable. */
export interface CodeRunner {
  run(source: string, stdin?: string): Promise<RunOutcome>;
}
