# Stackcraft

*Craft your stack, one internal at a time.*

A hostable learning platform. The first series is **Java Core** (collections, memory, strings, OOP, streams,
concurrency, JVM). DSA, LLD, HLD and Cloud follow as new `content/<series>/` folders, and those later series use this
codebase itself as their case study, so it is built like a real product: SOLID in UI and backend, ADRs, API-first,
12-factor config.

```
           browser
              |
     [ web ]  React SPA (static; nginx/CDN)          apps/web
              |  /api/v1/*
     [ api ]  Spring Boot: content, grading, XP       services/api
         |            \
   [ Postgres ]     [ runner ]  compiles + runs Java  services/runner
   (H2 in dev)       no DB, no secrets, hard limits
```

**Full docs: [docs/README.md](docs/README.md)** (getting started, build and test, architecture chapters, authoring).

Decisions: [ADR 0001 three services](docs/adr/0001-monorepo-three-services.md) ·
[ADR 0002 content as code](docs/adr/0002-content-as-code.md) ·
[ADR 0003 runner isolation](docs/adr/0003-runner-isolation.md) ·
[API contract](docs/api/openapi.yaml) · [Style guide](docs/STYLE-GUIDE.md)

**Try it on any device:** https://janigam.github.io/stackcraft/ (static preview: everything except live code runs; progress is saved in your browser).

## Run it locally (no Docker)

Needs JDK 21 and Node 24. Maven is not required (each service has a wrapper). Three terminals:

```bash
cd services/runner && ./mvnw spring-boot:run     # :8091, internal only (binds 127.0.0.1)
cd services/api && ./mvnw spring-boot:run        # :8080, dev profile: H2 file DB + content hot reload
cd apps/web && npm ci && npm run dev             # :5173, proxies /api to :8080
```

Open http://localhost:5173. In the Claude desktop app the same three are in `.claude/launch.json`.

No backend at all? `npm run dev` alone (or `?offline` in the URL) runs **offline mode**: content from the bundle,
progress in the browser, code blocks show their verified output.

## Run the whole stack with Docker

```bash
docker compose -f infra/docker-compose.yml up --build
```

Open http://localhost:8088. Postgres, api, runner and web; the runner sits on an internal network only.

## Checks

```bash
cd services/runner && ./mvnw verify    # sandbox limits, queue backpressure (429), ArchUnit
cd services/api && ./mvnw verify       # grader contract, idempotent XP, circuit breaker, OpenAPI match, ArchUnit
cd apps/web && npm test && npm run build
node scripts/verify-content.mjs        # schemas + re-runs every snippet against its stored output
```

## Editing a chapter

A chapter is a folder: `content/java-core/u00-rescue/l01-collections-map/`

| File | Section |
|---|---|
| `chapter.yaml` | metadata, "where it's asked", XP |
| `theory.mdx` | 1. Theory: hook, analogy, diagrams, runnable code, pros/cons, cheat card |
| `teasers.yaml` | 2. Brain teasers (commit before reveal; wrong goes to the Trap Board) |
| `questions.yaml` | 3. Question bank (warm-up MCQ, interview predict/short, killer problems) + flashcards |
| `code/X.java`, `code/X.out` | runnable demo and its verified output |

Edit, then run `node scripts/verify-content.mjs` (use `--write-out` after changing a demo, and review the diff). In
dev the api reloads content within a second and Vite hot-reloads theory.

## Layout

```
apps/web/src/
  app/        composition root (services.tsx picks remote or offline adapters), routing, theme
  pages/      Home, Series, Chapter, Trap Board, Kit showcase (/kit)
  features/   theory (MDX + widgets), teasers, quiz (renderer registry), code-runner, progress, flashcards
  entities/   chapter (ContentService port + adapters), question (graders mirror), learner (ranks)
  shared/     kit (A + D diagram kit), ui (atoms), api (types generated from openapi.yaml)
services/api/src/main/java/dev/stackcraft/api/
  content/    domain · application · port (ContentSource) · adapter (fs, web)
  quiz/       domain (sealed Question) · grading (Grader strategy + GraderRegistry) · application · adapter
  progress/   domain (XpEvent log, Rank, Leitner) · port (ProgressReader/Writer) · application · adapter (jpa, web)
  execution/  port (CodeExecutor) · application · adapter (RunnerHttpClient: timeouts + circuit breaker)
services/runner/src/main/java/dev/stackcraft/runner/
  domain · application (RunCodeUseCase, bounded queue) · port (Compiler, Sandbox) · adapter (javac, process, web)
```

## Configuration (12-factor)

| Variable | Service | Default |
|---|---|---|
| `STACKCRAFT_CONTENT_ROOT` | api | `../../content` |
| `STACKCRAFT_RUNNER_URL` | api | `http://127.0.0.1:8091` |
| `STACKCRAFT_WEB_ORIGINS` | api | `http://localhost:5173` |
| `SPRING_PROFILES_ACTIVE` | api | `dev` (H2) / `prod` (Postgres, JSON logs) |
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | api (prod) | |
| `RUNNER_TIMEOUT`, `RUNNER_MAX_HEAP_MB`, `RUNNER_OUTPUT_CAP`, `RUNNER_MAX_CONCURRENT`, `RUNNER_QUEUE_CAPACITY` | runner | 10s, 128, 64KB, 2, 8 |
| `VITE_API_PROXY` | web dev server | `http://localhost:8080` |
