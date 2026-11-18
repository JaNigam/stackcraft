# 3. The api service

`services/api/src/main/java/dev/stackcraft/api/`

## Hexagonal, package by feature

```
content/     domain · application · port · adapter/fs · adapter/web · ContentConfig
quiz/        domain · grading · application · adapter · QuizConfig
progress/    domain · port · application · adapter/jpa · adapter/web · ProgressConfig
execution/   port · application · adapter · ExecutionConfig
shared/      config properties, CORS, clock, request id, error handler, CurrentLearner
```

Each feature has the same layers:

| Layer | Contains | May depend on |
|---|---|---|
| `domain` | records and rules (`Question`, `XpEvent`, `Rank`, `FlashcardState`, `Chapter`) | other domains only; **no Spring, no JPA, no Jackson** |
| `port` | interfaces the use cases need (`ContentSource`, `ProgressReader/Writer`, `CodeExecutor`) | domain |
| `application` | one use case per class (`SubmitAnswerUseCase`, `AwardXpUseCase`, ...) | domain, ports; **no Spring** |
| `adapter` | HTTP controllers, JPA entities and repositories, file loader, HTTP client | anything |
| `*Config` | the composition root: `@Bean` methods that plug adapters into ports | anything |

`ArchitectureTest` (ArchUnit) fails the build if a domain class imports Spring, if a use case touches an adapter, or
if anything outside `adapter.jpa` touches JPA.

Use cases are plain classes wired by `@Bean` methods, not `@Service`. That keeps them framework-free and trivial to
unit-test with in-memory fakes (`InMemoryProgressStore`).

## The quiz engine (Strategy + Registry)

```java
public sealed interface Question permits Mcq, PredictOutput, ShortAnswer, Problem { ... }

public interface Grader<T extends Question> {
    Class<T> supports();
    GradeResult grade(T question, Answer answer);
}
```

- Each grader is a Spring bean. `QuizConfig` asks Spring for `List<Grader<? extends Question>>` and builds a
  `GraderRegistry`, which maps each runtime type to its grader.
- The registry checks at start-up that **every** permitted `Question` subtype has exactly one grader.
- `QuestionFactory` (adapter) builds the right record from YAML via a map of type → builder.
- `QuestionView` uses an exhaustive `switch` over the sealed type, so a new subtype without a JSON mapping does not
  compile.

## Progress (event log)

Progress is an append-only log of `XpEvent`s. Total XP, rank, streak, completed chapters and best scores are all
**derived** by `LearnerProgress.from(events, today)`, a pure function. See [Data and progress](07-data-and-progress.md).

## Errors

`ApiErrorHandler` turns exceptions into RFC 9457 problem documents with stable `type` URIs:

| Exception | Status | type |
|---|---|---|
| `NotFoundException` | 404 | `.../not-found` |
| `IllegalArgumentException`, validation | 400 | `.../bad-request` |
| `ExecutorBusyException` | 429 + `Retry-After` | `.../runner-busy` |
| `ExecutorUnavailableException` | 503 | `.../runner-unavailable` |

## Configuration

`StackcraftProperties` binds `stackcraft.*`; every key is an environment variable (`STACKCRAFT_RUNNER_URL`, ...).
Profiles: `dev` (default: H2 file DB, content reload) and `prod` (Postgres from `DATABASE_*`, ECS JSON logs).

## Interview angle

"How would you add a new question type without touching existing code?" Walk through the sealed interface, the
grader bean, the registry, and the contract test.
