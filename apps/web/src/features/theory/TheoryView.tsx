import { lazy, Suspense, useMemo, type ComponentType } from 'react';
import { MDXProvider } from '@mdx-js/react';
import { chapterFolder, contentRelative } from '@/entities/chapter/bundledContent';
import { Spinner } from '@/shared/ui';
import { ChapterContextProvider } from './ChapterContext';
import { mdxComponents } from './mdxComponents';

/** theory.mdx files compiled by Vite, code-split per chapter. */
const modules = import.meta.glob<{ default: ComponentType }>('@content/**/theory.mdx');
const byFolder = new Map(Object.entries(modules).map(([k, load]) => [contentRelative(k).replace(/\/theory\.mdx$/, ''), load]));
const cache = new Map<string, ComponentType>();

function theoryFor(folder: string): ComponentType | undefined {
  const load = byFolder.get(folder);
  if (!load) return undefined;
  if (!cache.has(folder)) cache.set(folder, lazy(load));
  return cache.get(folder);
}

export function TheoryView({ chapterId }: { chapterId: string }) {
  const Theory = useMemo(() => {
    const folder = chapterFolder(chapterId);
    return folder ? theoryFor(folder) : undefined;
  }, [chapterId]);

  if (!Theory) return <p className="k-muted">No theory written for this chapter yet.</p>;
  return (
    <ChapterContextProvider chapterId={chapterId}>
      <MDXProvider components={mdxComponents}>
        <Suspense fallback={<Spinner label="Loading theory" />}>
          <article className="th">
            <Theory />
          </article>
        </Suspense>
      </MDXProvider>
    </ChapterContextProvider>
  );
}
