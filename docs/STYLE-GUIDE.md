# Stackcraft style guide (locked: "A + D")

Live showcase: run the web app and open `/kit`. Code: `apps/web/src/shared/kit/`.

## The three visual modes
| Mode | Use for | Never use for |
|---|---|---|
| **A. Sticker-doodle characters** | intuition: the analogy scene, "who keeps what", moods when something is accepted or rejected | exact field names or numbers |
| **D. Interactive playgrounds** | anything the learner can operate: add elements, step a process, move a slider, choose a path | static facts a sentence says better |
| **Chalkboard panels** | exact internals only: real field names (`table[]`, `Node.next`), array slots, bit maths | analogies, decoration |

## Look
- Outlines: ink `#2C2C2A`, 2.5 to 3 px (`--stroke: 2.75px`; SVG strokes ~3.2 in a 124-unit viewBox).
- Fills (pastel): pink `#F4C0D1`, lavender `#CECBF6`, mint `#9FE1CB`, amber `#FAC775`, sky `#B5D8F5`, coral `#F5A08C`.
- Cards: cream `#FFF8EC`, hard offset shadow `4px 4px 0 ink`, radius 14 px.
- Chalkboard: board `#1F3A30`, wooden frame `#5B4636`, chalk `#F1EFE8`, accents chalk-yellow / pink / mint, mono font.
- Fonts (self-hosted): Fredoka (display), Nunito (body), JetBrains Mono (code).
- Family colours are fixed: List = mint, Set = lavender, Map = amber, Queue = sky, objects/blobs = pink.
- Characters have googly eyes and a mood: `happy`, `smug`, `surprised`, `sad`, `nope`, `sleepy`.
- Dark mode: page and cards go dark; **stickers keep their pastel fill and ink outline** and gain a light die-cut edge.

## Motion
- Short and bouncy: drops are springs (~0.3 s), hops are 0.45 s. Nothing loops except the machine gear.
- `prefers-reduced-motion`: no drops, hops or flips; state changes are instant.

## Accuracy rule
Every number in a diagram, reveal or answer comes from running code: either quoted from a verified `.out`, or
computed by kit code that is unit-tested against JVM output (`javaHash.ts`: `"apple"` → 93029210 → bucket 1 of 16).

## Layout
- Works at 375 px wide with a 16 px gutter and no horizontal page scroll (wide things scroll inside their own box).
- Tap targets at least 40 px high.

## Chapter writing order
Hook (interview moment) → analogy scene → theory with diagrams → runnable code, piece by piece → break-it
experiments → pros/cons table and why → cheat card. Teasers and the question bank live in their own YAML files.
