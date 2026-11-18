import { isValidElement, type ReactElement, type ReactNode } from 'react';
import type { MDXComponents } from 'mdx/types';
import * as kit from '@/shared/kit';
import { Badge, Callout } from '@/shared/ui';
import { CodeBlock, OutputBlock } from '@/shared/ui/CodeBlock';
import { Explain, Panel, Split } from '@/shared/ui/Split';
import { RunnableCode } from '@/features/code-runner/RunnableCode';
import { CollectionsPlayground } from './widgets/CollectionsPlayground';
import { HashMathBoard } from './widgets/HashMathBoard';
import { CollectionsFamilyTree } from './widgets/CollectionsFamilyTree';
import { CollectionPicker } from './widgets/CollectionPicker';

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
    <section className="th-cheat">
      <div className="th-cheat__pin" aria-hidden="true" />
      <h3>{title}</h3>
      {children}
    </section>
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
};
