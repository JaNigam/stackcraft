# 7. Data and progress

## Schema (Flyway `V1__progress.sql`)

| Table | Key columns | Notes |
|---|---|---|
| `xp_event` | `attempt_id` **unique**, `learner_id`, `kind`, `chapter_id`, `item_id`, `xp`, `score`, `occurred_at` | append-only log; index on `(learner_id, occurred_at)` |
| `trap_entry` | unique `(learner_id, chapter_id, item_id)`, `times_fallen`, `last_fallen_at`, `cleared` | the Trap Board |
| `flashcard_state` | unique `(learner_id, card_key)`, `box`, `due_on` | Leitner boxes |

The SQL runs unchanged on Postgres and on H2 in `MODE=PostgreSQL`. Hibernate runs with `ddl-auto=validate`: Flyway
owns the schema, JPA only checks it.

## Derived, not stored

There is no "total XP" column. `LearnerProgress.from(events, today, zone)` computes:

- total XP and rank (Trainee 0, Developer 200, Senior 500, Staff 900, Principal 1300) and XP to the next rank;
- streak: consecutive days with an event, counting back from today (yesterday keeps it alive until midnight);
- completed chapters (events of kind `CHAPTER`);
- best score per item.

Derived state can never drift from the log, and the function is pure, so it is trivially testable.

## XP rules (`AwardXpUseCase`)

```
earned  = round(item.maxXp × score)
paid    = sum of XP already awarded for (kind, chapter, item)
awarded = max(0, earned − paid)
```

- **Idempotent:** the same `attemptId` returns the first result with `duplicate: true`.
- **Race-safe:** two identical requests racing both pass the "seen?" check; the unique constraint rejects the second
  insert; the adapter turns that into `DuplicateAttemptException`; the use case replays the stored event.
- **No farming:** answering an MCQ ten times is still worth one MCQ; improving a partial score tops up to the max.
- Values: lesson 10, warm-up 2, interview 4, killer 8 (hints cost 20% each, floor 40%), teaser 1, flashcard 1 per
  card per day.

## Trap Board and flashcards

- A wrong teaser commit, or a zero-score answer, records a fall (or increments `times_fallen`). A later correct
  answer clears it. Duplicates do not touch the board.
- Completing a chapter unlocks its flashcards. Remembered moves a card up a box (due in 1, 2, 4, 8, 16 days);
  forgotten sends it back to box 1.

## Learners

v1 is single-learner: `CurrentLearner.LOCAL` returns `"local"`. Every table already has `learner_id`, so the auth
chapter adds a JWT-backed `CurrentLearner` and nothing else changes.

## Interview angle

"Event sourcing or a counter column?" Here: a small append-only log with derived reads, a unique key for
idempotency, and a clear path to snapshots if the log ever gets large.
