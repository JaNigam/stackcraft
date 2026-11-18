# ADR 0002: Content as code

- Status: Accepted
- Date: 2026-11-19

## Context
Chapters must be easy to edit on request ("enhance u04 l03 teasers") and must never contain a wrong number: every
output or bucket index shown to the learner has to come from a real JVM run.

## Decision
Chapters are files in `content/<series>/<unit>/<lesson>/`, reviewed in Git:

- `chapter.yaml` metadata, `teasers.yaml`, `questions.yaml` (questions + flashcards): structured, graded data.
- `theory.mdx`: prose plus kit components by name. It imports nothing, so content stays data.
- `code/*.java` with a verified `code/*.out` next to each.

Two readers, one format:

- The **api** loads the YAML at start-up behind the `ContentSource` port (`FileSystemContentSource`), fails fast on
  bad content, and in the `dev` profile hot-reloads on change, keeping the last good version if an edit is broken.
- The **web** build compiles `theory.mdx` and bundles the YAML and `.out` files. That gives an offline mode
  (`BundledContentService` + `LocalProgressService`) and a stored-output fallback when the runner is down.

`scripts/verify-content.mjs` schema-checks every file, re-runs every `code/*.java` against its `.out`, and runs every
`predict` question's code against its `expected`. It is the gate for any content change.

## Consequences
- Editing a chapter needs no code change; a broken chapter fails the check, not a learner's page.
- Answers are in the payload and in the public repo. That is acceptable: this is a study tool, the content is
  public anyway, and the UI only reveals answers after a commit. If competitive scoring ever matters, a public view
  that strips answers is one adapter change.
- The YAML parsing exists twice (Java and TypeScript). Both use the same defaults and are covered by tests; moving
  content to a DB or S3 later means one new `ContentSource` adapter on the api side.
