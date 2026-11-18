# 8. Resilience and security

## Failure modes and what the learner sees

| Failure | Mechanism | Learner sees |
|---|---|---|
| runner down | connect timeout (1 s) → 503 `runner-unavailable` | "Stored output" with the verified output |
| runner hung | read timeout (15 s) → 503 | same |
| runner keeps failing | Resilience4j circuit opens after 50% of the last 10 calls fail (min 3); fails fast for 15 s; then 2 trial calls; it leaves OPEN on a timer so `/runs/status` recovers by itself | instant fallback, no waiting on a dead service |
| runner full | runner 429 → api 429 `runner-busy` (not counted as a breaker failure) | "busy", stored output |
| snippet loops / floods / eats memory | 10 s wall clock, 64 KB output cap, 128 MB heap | TIMEOUT / OUTPUT_LIMIT / RUNTIME_ERROR badge |
| api down | the web probes at boot | **Offline mode**: bundle + browser storage |
| network retry | client `attemptId` | XP never doubles (`duplicate: true`) |
| bad content edit | fail-fast load; dev reload keeps the last good version | the page keeps working |

## Security baseline

- **Least privilege for code execution:** separate service, no DB, no secrets, empty environment for child JVMs,
  bound to loopback by default, internal Docker network only, unprivileged user; compose adds read-only root,
  `cap_drop: ALL`, `no-new-privileges`, pids and memory limits.
- **Input limits:** 64 KB source, 8 KB stdin and text answers, validated at the api edge before a runner slot is
  spent; nginx caps bodies at 128 KB.
- **CORS** locked to `STACKCRAFT_WEB_ORIGINS`, `GET`/`POST` only.
- **nginx headers:** CSP `default-src 'self'`, `nosniff`, strict referrer policy.
- **Errors** never include stack traces (`server.error.include-stacktrace=never`); problem documents only.
- **Request id** values are validated (`[A-Za-z0-9-]{1,64}`) before they reach logs or headers.
- **Secrets:** none in the repo; database credentials come from the environment.
- **Deferred on purpose:** authentication (its own chapter) and the per-run container sandbox (ADR 0003, before any
  public hosting).

## Observability

- Actuator `health` with liveness and readiness groups (Kubernetes-ready), `info`, `metrics`.
- `stackcraft.runner.run` timer tagged by status in the runner.
- `X-Request-Id` in every log line (`[%X{requestId}]`), forwarded api → runner; ECS JSON logs in `prod`.

## Interview angle

"Your downstream dependency is flaky. What do you do?" Timeouts first, then a circuit breaker that ignores
back-pressure signals, then a degraded experience that is still useful.
