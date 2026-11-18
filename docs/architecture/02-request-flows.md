# 2. Request flows

## Start-up: online or offline?

```mermaid
sequenceDiagram
  participant M as main.tsx
  participant S as chooseServices()
  participant A as api
  M->>S: boot
  S->>A: GET /api/v1/series (1.5 s timeout)
  alt api answers JSON
    S-->>M: online: Remote{Content,Progress}Service + RemoteCodeRunner
  else timeout / error
    S-->>M: offline: BundledContentService + LocalProgressService + OfflineCodeRunner
  end
  M->>M: render <App services=...>
```

The choice is made once, in the composition root (`app/services.tsx`). No component ever checks the mode to decide
how to fetch; it just calls the interface.

## Opening a chapter

1. `ChapterPage` calls `useChapter(id)` → `ContentService.getChapter` → `GET /api/v1/chapters/{id}`.
2. `ContentController` → `GetChapterUseCase` → `ContentSource.chapter(id)` (an in-memory snapshot; no disk read
   per request).
3. `QuestionView.of` maps each sealed `Question` to a flat JSON object with a `type` field.
4. Theory is not in that response: `TheoryView` lazy-loads the chapter's compiled `theory.mdx` chunk from the bundle.

## Answering a question

```mermaid
sequenceDiagram
  participant UI as McqCard
  participant H as useSubmitAnswer
  participant C as QuizController
  participant U as SubmitAnswerUseCase
  participant G as GraderRegistry
  participant X as AwardXpUseCase
  participant DB as ProgressStore
  UI->>H: onSubmit({choice: 3})
  H->>C: POST .../answers {attemptId: uuid, choice: 3}
  C->>U: submit(learner, chapter, question, attemptId, answer)
  U->>G: grade(question, answer)
  G-->>U: GradeResult(correct, score, ...)
  U->>X: award(claim)
  X->>DB: eventByAttempt(attemptId)?
  alt already seen
    X-->>U: duplicate (original XP)
  else new
    X->>DB: append(XpEvent)  [unique attempt_id]
    X-->>U: awarded = earned - already paid for this item
  end
  U->>DB: trap fell / escaped (only if not duplicate)
  C-->>H: {correct, feedback, xpAwarded, duplicate, progress}
  H->>H: setQueryData(progress), announce +XP
```

The `attemptId` is created once per click. TanStack Query retries the mutation (up to 2 times) with the **same**
variables, so a flaky network can retry safely: the server answers `duplicate: true` and XP is never doubled.

## Pressing Run

1. `RunnableCode` → `CodeRunner.run(source)` → `POST /api/v1/runs`.
2. `RunController` (api) validates size → `RunSnippetUseCase` → `CodeExecutor` = `RunnerHttpClient`.
3. `RunnerHttpClient` goes through the circuit breaker, adds `X-Request-Id`, and calls
   `POST runner:8091/internal/v1/runs` with a 1 s connect and 15 s read timeout.
4. Runner: `RunCodeUseCase` admits the run into the bounded pool (or 429), creates a temp folder, compiles in-process,
   runs a child JVM with limits, deletes the folder, returns `RunResult`.
5. Outcomes reaching the UI:
   - `200` with `status` OK / COMPILE_ERROR / RUNTIME_ERROR / TIMEOUT / OUTPUT_LIMIT → shown with a badge;
   - `429 runner-busy` → "busy", stored output shown;
   - `503 runner-unavailable` (down, timeout, or circuit open) → stored output shown with a "Stored output" badge.

## Interview angle

"How do you make a POST safe to retry?" Client-generated idempotency key, unique constraint as the real guard,
replay the original result on a duplicate. Same pattern as payment APIs.
