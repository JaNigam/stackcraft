# 5. The web app

`apps/web/src/`, feature-sliced:

```
app/        composition root (services.tsx), routing (App.tsx), theme tokens
pages/      HomePage, SeriesPage, ChapterPage, TrapBoardPage, KitPage
features/   theory/ (MDX + widgets)  teasers/  quiz/  code-runner/  progress/  flashcards/
entities/   chapter/ (ContentService port + adapters)  question/ (graders mirror)  learner/ (ranks)
shared/     kit/ (diagram kit)  ui/ (atoms, Split, CodeBlock)  api/ (generated types + client)
```

Import direction: `app → pages → features → entities → shared`. Shared never imports features.

## Ports and adapters in the UI

| Port | Online adapter | Offline adapter |
|---|---|---|
| `ContentService` | `RemoteContentService` (api) | `BundledContentService` (YAML compiled into the site) |
| `ProgressService` | `RemoteProgressService` (api) | `LocalProgressService` (same rules, `localStorage`) |
| `CodeRunner` | `RemoteCodeRunner` (`/api/v1/runs`) | `OfflineCodeRunner` (always "unavailable") |

`chooseServices()` picks a set once at boot; `ServicesProvider` injects it; features call `useContent()`,
`useProgressService()`, `useCodeRunner()`. Tests inject fakes the same way (`renderWithServices`).

## Containers and presenters

Fetching and state live in hooks (`useChapter`, `useProgress`, `useSubmitAnswer`), drawing lives in components.
Progress hooks are small and focused (`useXp`, `useStreak`, `useCompleted`, `useTrapBoard`) over one cached
TanStack Query, so a component re-renders only for what it reads.

## The question renderer registry

```ts
export const questionRenderers: Record<QuestionType, ComponentType<QuestionRendererProps>> = {
  mcq: McqCard, predict: PredictCard, short: ShortAnswerCard, problem: ProblemCard,
};
```

`QuestionType` is generated from `openapi.yaml`, so adding a type to the contract without a renderer is a compile
error. `QuestionList` never changes when a type is added. Each renderer receives `{question, onSubmit, best}` and
only says what the learner answered; the container adds the `attemptId`.

## Theory rendering

- `theory.mdx` files are compiled by `@mdx-js/rollup` (with `remark-gfm` for tables) and code-split per chapter
  (`import.meta.glob`, lazy).
- `mdxComponents.tsx` is the whitelist of components a chapter may use by name: the kit, callouts, `Split`,
  `RunnableCode`, chapter widgets. Content imports nothing.
- `ChapterContext` tells `RunnableCode` which chapter folder to read code files from.

## The diagram kit

`shared/kit/` holds presentational pieces only: `Character` (SVG sticker with googly eyes and moods), `Scene`/`Actor`,
`Bucket`, `Chain`/`NodeBox`, `Chalkboard`, `Stepper`, `Playground`, `StackFrame`/`HeapBlob`/`Machine`,
`HierarchyTree`, and `javaHash.ts` (Java's hash maths, tested against JVM output). Showcase: `/kit`. Rules:
[STYLE-GUIDE](../STYLE-GUIDE.md).

## Themes

`app/themes/*.theme.ts` are data files (id, label, light/dark, a value for every token in `THEME_TOKENS`).
`themes/registry.ts` finds them with `import.meta.glob`; `app/theme.ts` writes the chosen theme's tokens as CSS
custom properties on `<html>` and sets `data-scheme`; `ThemeMenu` lists the registry. Adding a theme is one file;
`themes.test.ts` checks completeness and contrast.

## Layout

`Split` / `Explain` / `Panel`: text left, sticky interactive panel right, stacked under 1024 px. Page width up to
1480 px with a fluid gutter; works down to 375 px with no page-level horizontal scroll.

## Chapter outline

`features/chapter-outline/` is the "where am I" panel on chapter pages: chapter progress (`chapterProgress`, a
pure function of the chapter and the learner's best scores, XP-weighted) plus every section. Sections register
themselves with a `data-outline` attribute (theory `h2`s get slug ids from `shared/lib/slugify`, the cheat card,
the question tiers), and `useOutlineEntries` collects them from the DOM, so content authors add nothing. The chapter
tabs stay mounted (`Tabs keepMounted`), which keeps every section discoverable and preserves widget state across
tab switches. A scroll-spy highlights the current section; clicks switch tab and scroll in a layout effect, and the
hash (`#theory/<slug>`, `#questions/tier-killer`) makes sections linkable. At 1200 px and up it is a sticky right
column; below that it is a "Contents" button that opens the same panel as a sheet.

## Interview angle

"How do you keep a React codebase from turning into a ball of mud?" Feature slices with a one-way import rule,
interfaces for anything with side effects, a composition root, and registries instead of `switch` statements.
