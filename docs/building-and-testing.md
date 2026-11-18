# Building and testing

Every part has one command that proves it works. Run the ones for what you touched; run all four before a merge.

```bash
cd services/runner && ./mvnw verify
```

```bash
cd services/api && ./mvnw verify
```

```bash
cd apps/web && npm test && npm run build
```

```bash
node scripts/verify-content.mjs
```

## What each suite proves

### Runner (`services/runner`)
| Test | Proves |
|---|---|
| `RunCodeUseCaseTest` | real javac + real child JVM: hello world, stdin, infinite loop killed at the wall clock, compile error without running, runtime error, output cap kills a print loop, heap cap, temp folders deleted |
| `RunQueueBackpressureTest` | with fake ports: 1 worker + 1 waiting slot, the third run is rejected (`RunQueueFullException`); oversized source rejected before queueing |
| `RunControllerTest` | HTTP mapping: 200 JSON, 429 with `Retry-After`, 400 on blank source |
| `RunnerApplicationTest` | full context: end-to-end run, `X-Request-Id` echoed, readiness probe |
| `ArchitectureTest` | domain has no Spring or adapters; application depends only on ports |

### Api (`services/api`)
| Test | Proves |
|---|---|
| `GraderContractTest` | the Liskov contract for every grader: never throws on hostile input, score in [0, 1], right answer scores 1, empty answer scores 0, pure; fails if a question type has no grader |
| `GraderBehaviourTest` | whitespace rules for predict-the-output, hint penalty, registry dispatch and start-up checks |
| `AwardXpUseCaseTest` | same `attemptId` awards once; a new attempt cannot farm XP; partial scores top up; the unique-constraint race is reported as a duplicate |
| `ProgressDomainTest` | rank thresholds, XP-to-next-rank, streak rules, Leitner box moves |
| `FileSystemContentSourceTest` | loads every question type; broken content fails with the folder name; hot reload picks up edits and keeps the last good version on a broken edit |
| `RunnerHttpClientTest` | against a real HTTP stub: result + request id forwarding, 429 is "busy" and does not trip the breaker, slow runner times out, dead runner opens the circuit and then fails fast |
| `ApiFlowTest` | the whole api with Flyway-migrated H2 and fixture content: series, chapter, idempotent answers, trap board, completion unlocks flashcards, 400s, 503 when the runner is down, CORS, health probes |
| `OpenApiContractTest` | every `METHOD /path` in `docs/api/openapi.yaml` is served, and nothing else under `/api/v1` is |
| `ArchitectureTest` | hexagonal rules: domain is plain Java, use cases talk to ports, only `adapter.jpa` touches JPA, controllers live in adapters, graders only grade |

### Web (`apps/web`)
| Test | Proves |
|---|---|
| `javaHash.test.ts` | the JS port of `String.hashCode`, spread and bucket index matches numbers printed by a real JVM |
| `graders.test.ts` | the offline grader mirror honours the same contract as the server |
| `LocalProgressService.test.ts` | offline rules match the api: idempotency, no farming, trap board, Leitner, ranks |
| `QuestionList.test.tsx` | the renderer registry covers every type; an MCQ and a predict answer grade end to end |
| `CollectionsPlayground.test.tsx` | the playground replays the tour exactly as `CollectionsTour.out` prints it |
| `RunnableCode.test.tsx` | runner unavailable shows the verified output; runner available shows the live result |

`npm run build` also type-checks (`tsc --noEmit`) and compiles every `theory.mdx`, so a broken component name in a
chapter fails the build.

### Content (`scripts/verify-content.mjs`)
Schema-checks every YAML file, re-runs every `code/*.java` and compares to its `.out`, and runs every `predict`
question's code against its `expected`. See [Authoring chapters](authoring-chapters.md).

## Regenerating the API types

`docs/api/openapi.yaml` is the source of truth. After changing it:

```bash
cd apps/web && npm run gen:api
```

That rewrites `src/shared/api/schema.d.ts`. Then `./mvnw verify` in `services/api` tells you whether the controllers
still match.

## Building images

```bash
docker compose -f infra/docker-compose.yml build
```

Each Dockerfile uses the repo root as build context (the api image bakes in `content/`, the web image bundles it).

## Rules of thumb

- Always wrap long Java runs in a timeout locally (`timeout 300 ./mvnw verify`).
- Compile before running stress-style tests.
- Never commit `.env`, `services/api/data/`, `node_modules/` or `target/` (all in `.gitignore`).
