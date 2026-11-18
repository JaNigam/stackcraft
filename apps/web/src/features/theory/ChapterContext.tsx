import { createContext, useContext, type ReactNode } from 'react';

/** Which chapter MDX is rendering, so <RunnableCode file="..."/> can find the chapter's code folder. */
const Ctx = createContext<{ chapterId: string } | null>(null);

export function ChapterContextProvider({ chapterId, children }: { chapterId: string; children: ReactNode }) {
  return <Ctx.Provider value={{ chapterId }}>{children}</Ctx.Provider>;
}

export function useChapterContext() {
  const c = useContext(Ctx);
  if (!c) throw new Error('ChapterContextProvider missing');
  return c;
}
