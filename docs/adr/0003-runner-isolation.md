# ADR 0003: Runner isolation

- Status: Accepted for local and single-learner use. **Must be revisited before public hosting.**
- Date: 2026-11-19

## Context
The learner runs and edits Java on a live JVM. That is arbitrary code execution by design.

## Decision
A separate `runner` service that does one thing (compile and run) behind two ports, `Compiler` and `Sandbox`:

- `JavacCompiler`: in-process `javax.tools` compiler, annotation processing off (`-proc:none`).
- `ProcessSandbox`: a child JVM per run with
  - 10 s wall clock, then the process and its descendants are killed (`TIMEOUT`);
  - `-Xmx128m`, serial GC, 1 MB thread stacks;
  - 64 KB cap per output stream; one byte more kills the process (`OUTPUT_LIMIT`);
  - an **empty environment** (only `SystemRoot` on Windows), so no inherited secrets;
  - a per-run temp folder, always deleted afterwards.
- A bounded worker pool (default 2 workers + 8 waiting). When full, the runner answers **429** with
  `Retry-After` immediately instead of queueing without limit (backpressure).
- No database, no credentials, bound to `127.0.0.1` by default; only reachable from the api's private network.
- The api calls it with timeouts and a Resilience4j circuit breaker; when the runner is down the UI shows the stored,
  verified output.

All limits are environment variables (`RUNNER_TIMEOUT`, `RUNNER_MAX_HEAP_MB`, ...).

## Honest limits
A child process still shares the host's file system, network and CPU with the runner. A snippet could read files the
runner user can read, open sockets, or spawn processes until the wall clock fires. That is acceptable on the learner's
own machine. It is **not** acceptable on the public internet.

## Before public hosting (Cloud series)
Replace `ProcessSandbox` with an adapter that runs each snippet in a throwaway sandbox: an ephemeral container with
no network, read-only root, a pids limit and cgroup CPU/memory quotas, ideally under gVisor or Firecracker. Because
the use case depends only on the `Sandbox` port, that is one new adapter and no other change.
