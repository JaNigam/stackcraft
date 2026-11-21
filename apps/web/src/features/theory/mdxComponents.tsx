import { isValidElement, type ReactElement, type ReactNode } from 'react';
import type { MDXComponents } from 'mdx/types';
import * as kit from '@/shared/kit';
import { Badge, Callout } from '@/shared/ui';
import { CodeBlock, OutputBlock } from '@/shared/ui/CodeBlock';
import { Explain, Panel, Split } from '@/shared/ui/Split';
import { slugify } from '@/shared/lib/slugify';
import { RunnableCode } from '@/features/code-runner/RunnableCode';
import { CollectionsPlayground } from './widgets/CollectionsPlayground';
import { HashMathBoard } from './widgets/HashMathBoard';
import { CollectionsFamilyTree } from './widgets/CollectionsFamilyTree';
import { CollectionPicker } from './widgets/CollectionPicker';
import { ThreeSumPlayground } from './widgets/ThreeSumPlayground';

/** Fenced ```java blocks arrive as <pre><code className="language-java">…</code></pre>. */
function Pre({ children }: { children?: ReactNode }) {
  if (isValidElement(children)) {
    const el = children as ReactElement<{ className?: string; children?: ReactNode }>;
    const lang = el.props.className?.replace('language-', '') ?? 'text';
    const text = String(el.props.children ?? '');
    return lang === 'output' ? <OutputBlock text={text} /> : <CodeBlock code={text} language={lang} />;
  }
  return <pre>{children}</pre>;
}

function CheatCard({ title = 'Cheat card', children }: { title?: string; children: ReactNode }) {
  return (
    <section className="th-cheat" id="cheat-card" data-outline="Cheat card">
      <div className="th-cheat__pin" aria-hidden="true" />
      <h3>{title}</h3>
      {children}
    </section>
  );
}

/** Plain text of a heading's children (headings may contain `code`). */
function textOf(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node)) return node.map(textOf).join('');
  if (isValidElement(node)) return textOf((node as ReactElement<{ children?: ReactNode }>).props.children);
  return '';
}

/** Section headings get a stable id and register themselves for the chapter outline (right-hand panel). */
function H2({ children }: { children?: ReactNode }) {
  const label = textOf(children);
  return (
    <h2 id={slugify(label)} data-outline={label}>
      {children}
    </h2>
  );
}

function Hook({ children }: { children: ReactNode }) {
  return <Callout kind="interview">{children}</Callout>;
}

/**
 * Everything a chapter's MDX may use, by name. Content stays data: theory.mdx imports nothing, so chapters can be
 * edited without touching app code, and a renamed component breaks the build instead of a page.
 */
export const mdxComponents: MDXComponents = {
  pre: Pre,
  h2: H2,
  table: (props) => (
    <div className="table-scroll">
      <table {...props} />
    </div>
  ),
  Callout,
  Badge,
  Split,
  Explain,
  Panel,
  CheatCard,
  Hook,
  RunnableCode,
  Scene: kit.Scene,
  Actor: kit.Actor,
  Character: kit.Character,
  Bucket: kit.Bucket,
  NodeBox: kit.NodeBox,
  Chain: kit.Chain,
  Chalkboard: kit.Chalkboard,
  Chalk: kit.Chalk,
  Stepper: kit.Stepper,
  Playground: kit.Playground,
  StackFrame: kit.StackFrame,
  HeapBlob: kit.HeapBlob,
  Machine: kit.Machine,
  CollectionsPlayground,
  HashMathBoard,
  CollectionsFamilyTree,
  CollectionPicker,
  ThreeSumPlayground,
};
