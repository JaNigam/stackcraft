import { bundledContent } from '@/entities/chapter/bundledContent';
import { grade } from '@/entities/question/graders';
import { rankFor } from '@/entities/learner/ranks';
import { ApiError } from '@/shared/api/http';
import type { AnswerBody, CommitBody, DueCard, ProgressView, ReviewBody, TrapView } from '@/shared/api/types';
import type { ProgressService } from './ProgressService';
import type { GuestImport } from '@/shared/api/types';

/**
 * Offline adapter: the same rules as the api (idempotent attemptId, no XP farming, trap board, Leitner boxes),
 * stored in the browser. Lets the site run as a static build with no backend at all.
 */
type Kind = 'ANSWER' | 'TEASER' | 'CHAPTER' | 'FLASHCARD';
interface XpEvent { attemptId: string; kind: Kind; chapterId: string; itemId: string; xp: number; score: number; at: string }
interface Trap extends TrapView { cleared: boolean }
interface Card { key: string; box: number; dueOn: string }
interface State { events: XpEvent[]; traps: Trap[]; cards: Card[] }

const KEY = 'stackcraft.progress.v1';
const INTERVALS = [1, 2, 4, 8, 16];
const ATTEMPT = /^[A-Za-z0-9-]{8,64}$/;

const isoDay = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const addDays = (day: string, n: number) => {
  const d = new Date(`${day}T00:00:00`);
  d.setDate(d.getDate() + n);
  return isoDay(d);
};

export class LocalProgressService implements ProgressService {
  readonly mode = 'offline' as const;

  constructor(
    private readonly storage: Storage = localStorage,
    private readonly now: () => Date = () => new Date(),
  ) {}

  private read(): State {
    try {
      const raw = this.storage.getItem(KEY);
      if (raw) return JSON.parse(raw) as State;
    } catch {
      /* corrupt or blocked storage: start fresh */
    }
    return { events: [], traps: [], cards: [] };
  }

  private write(s: State) {
    try {
      this.storage.setItem(KEY, JSON.stringify(s));
    } catch {
      /* storage full or blocked: progress for this session only */
    }
  }

  private award(s: State, attemptId: string, kind: Kind, chapterId: string, itemId: string, maxXp: number, score: number) {
    if (!ATTEMPT.test(attemptId)) throw new ApiError(400, 'bad-request', 'attemptId must be 8-64 characters of letters, digits or -');
    const existing = s.events.find((e) => e.attemptId === attemptId);
    if (existing) return { xpAwarded: existing.xp, duplicate: true };
    const paid = s.events.filter((e) => e.kind === kind && e.chapterId === chapterId && e.itemId === itemId).reduce((n, e) => n + e.xp, 0);
    const xp = Math.max(0, Math.round(maxXp * score) - paid);
    s.events.push({ attemptId, kind, chapterId, itemId, xp, score, at: this.now().toISOString() });
    return { xpAwarded: xp, duplicate: false };
  }

  private fell(s: State, chapterId: string, itemId: string, kind: 'question' | 'teaser', prompt: string) {
    const at = this.now().toISOString();
    const t = s.traps.find((x) => x.chapterId === chapterId && x.itemId === itemId);
    if (t) Object.assign(t, { timesFallen: t.timesFallen + 1, lastFallenAt: at, cleared: false });
    else s.traps.push({ chapterId, itemId, kind, prompt: prompt.replace(/\s+/g, ' ').trim().slice(0, 300), timesFallen: 1, lastFallenAt: at, cleared: false });
  }

  private escaped(s: State, chapterId: string, itemId: string) {
    const t = s.traps.find((x) => x.chapterId === chapterId && x.itemId === itemId);
    if (t) t.cleared = true;
  }

  private progressOf(s: State): ProgressView {
    const total = s.events.reduce((n, e) => n + e.xp, 0);
    const { rank, next } = rankFor(total);
    const days = new Set(s.events.map((e) => isoDay(new Date(e.at))));
    const today = isoDay(this.now());
    let cursor = days.has(today) ? today : addDays(today, -1);
    let streak = 0;
    while (days.has(cursor)) {
      streak++;
      cursor = addDays(cursor, -1);
    }
    const best: Record<string, number> = {};
    for (const e of s.events) {
      if (e.kind === 'ANSWER' || e.kind === 'TEASER') {
        const k = `${e.chapterId}/${e.itemId}`;
        best[k] = Math.max(best[k] ?? 0, e.score);
      }
    }
    return {
      learnerId: 'local',
      totalXp: total,
      rank: rank.id,
      rankLabel: rank.label,
      rankMinXp: rank.minXp,
      nextRank: next?.id ?? null,
      nextRankLabel: next?.label ?? null,
      nextRankMinXp: next?.minXp ?? null,
      xpToNextRank: next ? next.minXp - total : 0,
      streakDays: streak,
      completedChapters: [...new Set(s.events.filter((e) => e.kind === 'CHAPTER').map((e) => e.chapterId))].sort(),
      bestScores: best,
    };
  }

  private chapter(chapterId: string) {
    const c = bundledContent().chapters.get(chapterId);
    if (!c) throw new ApiError(404, 'not-found', `Chapter '${chapterId}' not found`);
    return c;
  }

  async getProgress() {
    return this.progressOf(this.read());
  }

  /**
   * What a guest earned in this browser, in the api's import shape (ADR 0004). XP numbers are left out on purpose:
   * the server re-derives them from the content. Returns null when there is nothing to import.
   */
  exportGuest(): GuestImport | null {
    const s = this.read();
    if (s.events.length === 0 && s.traps.length === 0 && s.cards.length === 0) return null;
    return {
      events: s.events.slice(-2000).map(({ attemptId, kind, chapterId, itemId, score, at }) => ({ attemptId, kind, chapterId, itemId, score, at })),
      traps: s.traps.filter((t) => !t.cleared).slice(-500).map(({ cleared: _c, ...t }) => t),
      cards: s.cards.slice(-1000),
    };
  }

  /** After a successful import: keep one backup copy, then start the next guest session empty. */
  clearAfterImport() {
    try {
      const raw = this.storage.getItem(KEY);
      if (raw) this.storage.setItem(`${KEY}.imported-backup`, raw);
      this.storage.removeItem(KEY);
    } catch {
      /* blocked storage */
    }
  }

  async submitAnswer(chapterId: string, questionId: string, body: AnswerBody) {
    const q = this.chapter(chapterId).view.questions.find((x) => x.id === questionId);
    if (!q) throw new ApiError(404, 'not-found', `Question '${questionId}' not found`);
    const s = this.read();
    const g = grade(q, body);
    const a = this.award(s, body.attemptId, 'ANSWER', chapterId, questionId, q.xp, g.score);
    if (!a.duplicate) {
      if (g.correct) this.escaped(s, chapterId, questionId);
      else if (g.score === 0) this.fell(s, chapterId, questionId, 'question', q.prompt);
    }
    this.write(s);
    return { ...g, ...a, progress: this.progressOf(s) };
  }

  async commitTeaser(chapterId: string, teaserId: string, body: CommitBody) {
    const t = this.chapter(chapterId).view.teasers.find((x) => x.id === teaserId);
    if (!t) throw new ApiError(404, 'not-found', `Teaser '${teaserId}' not found`);
    if (body.choice < 0 || body.choice >= t.options.length) throw new ApiError(400, 'bad-request', 'choice out of range');
    const s = this.read();
    const correct = body.choice === t.answer;
    const a = this.award(s, body.attemptId, 'TEASER', chapterId, teaserId, t.xp, correct ? 1 : 0);
    if (!a.duplicate) {
      if (correct) this.escaped(s, chapterId, teaserId);
      else this.fell(s, chapterId, teaserId, 'teaser', t.prompt);
    }
    this.write(s);
    return { correct, answer: t.answer, reveal: t.reveal, ...a, progress: this.progressOf(s) };
  }

  async completeChapter(chapterId: string, attemptId: string) {
    const c = this.chapter(chapterId);
    const s = this.read();
    const a = this.award(s, attemptId, 'CHAPTER', chapterId, 'complete', c.view.meta.completionXp, 1);
    this.write(s);
    return { ...a, progress: this.progressOf(s) };
  }

  async getTrapBoard() {
    return this.read()
      .traps.filter((t) => !t.cleared)
      .sort((a, b) => b.timesFallen - a.timesFallen || b.lastFallenAt.localeCompare(a.lastFallenAt))
      .map(({ cleared: _cleared, ...view }) => view);
  }

  async getDueFlashcards(limit = 5): Promise<DueCard[]> {
    const s = this.read();
    const today = isoDay(this.now());
    const done = new Set(s.events.filter((e) => e.kind === 'CHAPTER').map((e) => e.chapterId));
    const due: DueCard[] = [];
    for (const [id, ch] of bundledContent().chapters) {
      if (!done.has(id)) continue;
      for (const f of ch.flashcards) {
        const key = `${id}/${f.id}`;
        const st = s.cards.find((c) => c.key === key) ?? { key, box: 1, dueOn: today };
        if (st.dueOn <= today) due.push({ key, chapterId: id, chapterTitle: ch.view.meta.title, front: f.front, back: f.back, box: st.box, dueOn: st.dueOn });
      }
    }
    return due.sort((a, b) => a.dueOn.localeCompare(b.dueOn)).slice(0, limit);
  }

  async reviewFlashcard(body: ReviewBody) {
    const [chapterId, cardId] = [body.cardKey.slice(0, body.cardKey.indexOf('/')), body.cardKey.slice(body.cardKey.indexOf('/') + 1)];
    if (!this.chapter(chapterId).flashcards.some((f) => f.id === cardId)) throw new ApiError(404, 'not-found', `Flashcard '${body.cardKey}' not found`);
    const s = this.read();
    const today = isoDay(this.now());
    const a = this.award(s, body.attemptId, 'FLASHCARD', chapterId, `${cardId}@${today}`, 1, body.remembered ? 1 : 0);
    let card = s.cards.find((c) => c.key === body.cardKey);
    if (!card) {
      card = { key: body.cardKey, box: 1, dueOn: today };
      s.cards.push(card);
    }
    if (!a.duplicate) {
      card.box = body.remembered ? Math.min(card.box + 1, 5) : 1;
      card.dueOn = addDays(today, INTERVALS[card.box - 1]);
    }
    this.write(s);
    return { box: card.box, dueOn: card.dueOn, ...a, progress: this.progressOf(s) };
  }
}
