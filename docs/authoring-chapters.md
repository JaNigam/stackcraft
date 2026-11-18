# Authoring chapters

A chapter is one folder. Nothing else in the repo needs to change to add or edit one.

```
content/java-core/
  series.yaml                         units in reading order, each lists its chapter folders
  u00-rescue/
    l01-collections-map/
      chapter.yaml                    metadata
      theory.mdx                      section 1: theory
      teasers.yaml                    section 2: brain teasers
      questions.yaml                  section 3: question bank + flashcards
      code/CollectionsTour.java       runnable demo
      code/CollectionsTour.out        its verified output
```

## The workflow

1. Edit the files.
2. If you changed a demo: `node scripts/verify-content.mjs --write-out`, then review the `.out` diff.
3. `node scripts/verify-content.mjs` must say OK.
4. With the dev stack running, the api reloads content within a second and Vite hot-reloads the theory.

## chapter.yaml

```yaml
id: java-core.u00.l01          # globally unique, dotted lowercase
lesson: 1
title: The Collections Map
subtitle: Every collection in one picture
minutes: 25
completionXp: 10               # course rule: 10 per lesson
whereAsked: [Goldman Sachs Superday, round 1]
prerequisites: []
codeFiles: [CollectionsTour.java]
```

## theory.mdx

Order (the teaching style): hook → analogy scene → theory with diagrams → runnable code piece by piece → break-it →
pros/cons table and why → cheat card.

Use `<Split>` for any section that has something to look at or operate: the explanation goes left, the panel right.

```mdx
<Split>
<Explain>

## Heading

Markdown with **bold**, `code` and tables.

</Explain>
<Panel>

<CollectionsPlayground />

</Panel>
</Split>
```

Leave a blank line after `<Explain>`/`<Panel>` and before their closing tags, or MDX treats the markdown as text.
Ratios: `<Split ratio="wide-panel">` for big widgets, `"wide-text"` for small ones.

Available components (see `apps/web/src/features/theory/mdxComponents.tsx` and the `/kit` page):

| Component | Use |
|---|---|
| `Hook`, `Callout kind="trap|java|spring|why|interview"`, `CheatCard` | text blocks |
| `Scene` + `Actor kind mood label says` | analogy strip |
| `Character`, `Bucket`, `Chain`, `NodeBox`, `StackFrame`, `HeapBlob`, `Machine`, `Stepper`, `Playground`, `Chalkboard`, `Chalk` | the diagram kit |
| `RunnableCode file="X.java" [startEditing]` | live Java with verified-output fallback |
| chapter widgets (`CollectionsPlayground`, `HashMathBoard`, ...) | interactive pieces in `features/theory/widgets/` |
| ```` ```java ```` and ```` ```output ```` fences | static code and quoted output |

Rule: every number you write must be quoted from a `.out` file or computed by kit code that is tested against a JVM.

## teasers.yaml

```yaml
teasers:
  - id: t1
    prompt: You add "apple" to a HashSet that already contains it. What does add() return?
    code: |                      # optional
      set.add("apple");
    options: ["true", "false", "throws"]
    answer: 1                    # index into options
    reveal: |                    # small markdown: `code`, **bold**, *italic*, paragraphs
      `false`: ...
    xp: 1                        # optional, default 1
```

## questions.yaml

```yaml
questions:
  - { id: w1, tier: warmup, type: mcq, prompt: ..., options: [...], answer: 3, explanation: ... }
  - id: i1
    tier: interview
    type: predict                # code must be a full program with a public class
    code: |
      public class P { public static void main(String[] a) { System.out.println(1 + 2 + "3"); } }
    expected: "33"               # verify-content runs the code and checks this
    explanation: ...
  - { id: i4, tier: interview, type: short, prompt: ..., modelAnswer: ..., keyPoints: [...] }
  - { id: k1, tier: killer, type: problem, prompt: ..., hints: [h1, h2, h3], modelAnswer: ... }
flashcards:
  - { id: f1, front: ..., back: ... }
```

Default XP by tier: warmup 2, interview 4, killer 8 (override with `xp:`). Problems need exactly 3 hints.

## Adding a new question type

That is a code change, and it is deliberately small (open/closed principle):

1. api: a new record in the sealed `Question` interface, a builder entry in `QuestionFactory`, a `Grader` bean, and a
   case in `QuestionView` (the compiler forces this one).
2. `docs/api/openapi.yaml`: add the type to `QuestionType`; run `npm run gen:api`.
3. web: a renderer in `features/quiz/renderers/` and one entry in `registry.ts` (the compiler forces this too), plus
   the offline grader in `entities/question/graders.ts`.
4. Add the type to `GraderContractTest.graders()` and to `verify-content.mjs`.
