# ADR 0001: One monorepo, three deployable services

- Status: Accepted
- Date: 2026-11-19

## Context
Stackcraft is a learning platform that must also be a teaching example: later LLD, HLD and Cloud chapters use its
own code and architecture as the case study, and the Cloud series deploys it. It has three very different jobs:
render an interactive UI, own learner data and grading, and execute untrusted Java code.

## Decision
One Git repository with three separately built, separately deployed services:

| Service | Owns | Runtime |
|---|---|---|
| `apps/web` | rendering, diagrams, offline mode | static files (nginx now, CDN later) |
| `services/api` | content delivery, grading, progress, XP, flashcards | Spring Boot 4, Java 21, Postgres (H2 in dev) |
| `services/runner` | compiling and running snippets, nothing else | Spring Boot 4, Java 21, no DB, no secrets |

The browser talks only to `api` (`/api/v1`). `api` reaches `runner` through the `CodeExecutor` port over a private
network. The contract lives in `docs/api/openapi.yaml`.

## Consequences
- One commit can change the contract, the UI and the content together (no version skew between repos).
- Each service scales and fails on its own: a runner overload cannot take down progress tracking, and the web
  bundle can even run with no backend (offline mode, see ADR 0002).
- More moving parts than a single app: three builds, three images. Accepted, because the split itself is the HLD
  lesson (gateway, internal network, blast radius).
- Rejected: a single Spring Boot app serving everything. Simpler to run, but untrusted code would execute next to the
  database and secrets.
