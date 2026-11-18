# 4. The runner service

`services/runner/src/main/java/dev/stackcraft/runner/`

```
domain/        RunRequest, RunResult, RunStatus, Limits, MainClassName
port/          Compiler, Sandbox
application/   RunCodeUseCase (bounded pool), RunQueueFullException, SourceTooLargeException
adapter/       compiler/JavacCompiler, sandbox/ProcessSandbox, web/RunController + error handler + request id
config/        RunnerProperties (env-driven limits), RunnerConfig (composition root)
```

## One run, step by step

1. **Admission.** `RunCodeUseCase.run` checks the source size, then submits to a `ThreadPoolExecutor` with
   `maxConcurrent` workers and an `ArrayBlockingQueue(queueCapacity)`. If both are full, `AbortPolicy` throws and the
   controller answers **429** with `Retry-After: 2`. No unbounded queue, no thread explosion.
2. **Workspace.** A fresh `run-*` folder under the temp root.
3. **Compile.** `MainClassName` finds the public class; `JavacCompiler` writes `Name.java` and compiles in-process
   with `-proc:none` (no annotation processors, so no code runs at compile time). Errors come back as
   `Name.java:3: error: ...`, with the temp path stripped.
4. **Run.** `ProcessSandbox` starts `java -Xmx128m -Xss1m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -cp <dir> Name`:
   - environment cleared (only `SystemRoot` kept on Windows), so no inherited secrets;
   - stdout and stderr drained by two virtual threads into capped buffers; one byte over 64 KB kills the process;
   - `waitFor(10 s)`; on timeout the process and all descendants are killed.
5. **Cleanup.** The folder is deleted in a `finally`, success or failure.
6. **Result.** `RunResult(status, stdout, stderr, exitCode, durationMs, truncated)`; the status is decided by the
   runner, so the UI never parses stderr.

## Limits (all environment variables)

| Variable | Default |
|---|---|
| `RUNNER_TIMEOUT` | `10s` |
| `RUNNER_MAX_HEAP_MB` | `128` |
| `RUNNER_OUTPUT_CAP` | `64KB` |
| `RUNNER_MAX_SOURCE` | `64KB` |
| `RUNNER_MAX_CONCURRENT` | `2` |
| `RUNNER_QUEUE_CAPACITY` | `8` |
| `RUNNER_BIND` | `127.0.0.1` |

## Honest limits

A child process still shares the host's file system and network. Fine on your own machine, not on the internet.
ADR 0003 records the plan: replace `ProcessSandbox` with an ephemeral-container adapter (no network, read-only root,
pids and cgroup limits, ideally gVisor). Only the adapter changes; the use case depends on the `Sandbox` port.

## Interview angle

"How do you protect a service from overload?" Bounded concurrency plus a bounded queue, reject early with 429 and
`Retry-After`, and let the caller (here, the api's circuit breaker) back off.
