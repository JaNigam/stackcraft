import { load } from 'js-yaml';
import type { ChapterMeta, ChapterView, Question, SeriesView, Teaser } from '@/shared/api/types';

/**
 * Build-time snapshot of content/ (same files the api reads). Parsed with the same defaults as the api's
 * ContentLoader so both adapters return identical shapes. Also maps chapter ids to folders, which the theory and
 * code-file lookups need.
 */
const rawYaml = import.meta.glob<string>('@content/**/*.yaml', { query: '?raw', import: 'default', eager: true });

/** "…/content/java-core/u00-rescue/l01-collections-map/chapter.yaml" -> "java-core/u00-rescue/l01-collections-map/chapter.yaml" */
export function contentRelative(key: string): string {
  const parts = key.split(/@content\/|\/content\//);
  return parts[parts.length - 1];
}

const files = new Map<string, string>(Object.entries(rawYaml).map(([k, v]) => [contentRelative(k), v]));

type Any = Record<string, unknown>;
const tierDefaultXp: Record<string, number> = { warmup: 2, interview: 4, killer: 8 };

function parse(path: string): Any | undefined {
  const text = files.get(path);
  return text === undefined ? undefined : ((load(text) as Any) ?? {});
}

export interface BundledChapter {
  view: ChapterView;
  folder: string; // e.g. java-core/u00-rescue/l01-collections-map
  flashcards: { id: string; front: string; back: string }[];
}

function buildChapter(folder: string, seriesId: string, unitId: string): BundledChapter {
  const c = parse(`${folder}/chapter.yaml`) ?? {};
  const meta: ChapterMeta = {
    id: String(c.id),
    seriesId,
    unitId,
    lesson: Number(c.lesson ?? 0),
    title: String(c.title ?? ''),
    subtitle: (c.subtitle as string | undefined) ?? null,
    minutes: Number(c.minutes ?? 20),
    completionXp: Number(c.completionXp ?? 10),
    whereAsked: (c.whereAsked as string[]) ?? [],
    prerequisites: (c.prerequisites as string[]) ?? [],
    codeFiles: (c.codeFiles as string[]) ?? [],
  };
  const teasers: Teaser[] = (((parse(`${folder}/teasers.yaml`) ?? {}).teasers as Any[]) ?? []).map((t) => ({
    id: String(t.id),
    prompt: String(t.prompt),
    code: (t.code as string | undefined) ?? null,
    options: t.options as string[],
    answer: Number(t.answer),
    reveal: String(t.reveal),
    xp: Number(t.xp ?? 1),
  }));
  const q = parse(`${folder}/questions.yaml`) ?? {};
  const questions: Question[] = ((q.questions as Any[]) ?? []).map((x) => {
    const tier = String(x.tier) as Question['tier'];
    const type = String(x.type) as Question['type'];
    const base = { id: String(x.id), type, tier, xp: Number(x.xp ?? tierDefaultXp[tier]) };
    switch (type) {
      case 'mcq':
        return { ...base, prompt: String(x.prompt), code: x.code as string | undefined, options: x.options as string[], answer: Number(x.answer), explanation: x.explanation as string };
      case 'predict':
        return { ...base, prompt: String(x.prompt ?? 'What does this print?'), code: String(x.code), expected: String(x.expected), explanation: x.explanation as string };
      case 'short':
        return { ...base, prompt: String(x.prompt), modelAnswer: String(x.modelAnswer), keyPoints: (x.keyPoints as string[]) ?? [] };
      case 'problem':
        return { ...base, prompt: String(x.prompt), hints: (x.hints as string[]) ?? [], modelAnswer: String(x.modelAnswer) };
    }
  });
  const flashcards = ((q.flashcards as Any[]) ?? []).map((f) => ({ id: String(f.id), front: String(f.front), back: String(f.back) }));
  return { view: { meta, teasers, questions, flashcardCount: flashcards.length }, folder, flashcards };
}

export interface BundledContent {
  series: SeriesView[];
  chapters: Map<string, BundledChapter>;
}

function buildAll(): BundledContent {
  const series: SeriesView[] = [];
  const chapters = new Map<string, BundledChapter>();
  const seriesFiles = [...files.keys()].filter((k) => /^[^/]+\/series\.yaml$/.test(k)).sort();
  for (const sf of seriesFiles) {
    const seriesDir = sf.split('/')[0];
    const s = parse(sf) ?? {};
    const units = ((s.units as Any[]) ?? []).map((u, i) => {
      const list = ((u.chapters as string[]) ?? []).map((folder) => {
        const ch = buildChapter(`${seriesDir}/${u.dir}/${folder}`, String(s.id), String(u.id));
        chapters.set(ch.view.meta.id, ch);
        const m = ch.view.meta;
        return { id: m.id, lesson: m.lesson, title: m.title, subtitle: m.subtitle, minutes: m.minutes };
      });
      return { id: String(u.id), number: Number(u.number ?? i), title: String(u.title ?? ''), summary: (u.summary as string | undefined) ?? null, chapters: list };
    });
    series.push({ id: String(s.id), title: String(s.title ?? ''), tagline: (s.tagline as string | undefined) ?? null, units });
  }
  return { series, chapters };
}

let cache: BundledContent | undefined;

export function bundledContent(): BundledContent {
  cache ??= buildAll();
  return cache;
}

export function chapterFolder(chapterId: string): string | undefined {
  return bundledContent().chapters.get(chapterId)?.folder;
}
