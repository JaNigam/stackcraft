import type { ReactNode } from 'react';

/**
 * Two-column section: explanation on the left, the thing you operate on the right. The right panel is sticky,
 * so it stays in view while you read. Below 1024 px it stacks (text first, panel second).
 *
 * MDX usage (blank lines around markdown inside JSX):
 *   <Split>
 *   <Explain>
 *   ## Heading
 *   Some **markdown**.
 *   </Explain>
 *   <Panel><CollectionsPlayground /></Panel>
 *   </Split>
 */
export function Split({ children, ratio = 'balanced' }: { children: ReactNode; ratio?: 'balanced' | 'wide-panel' | 'wide-text' }) {
  return <section className={`ui-split ui-split--${ratio}`}>{children}</section>;
}

export function Explain({ children }: { children: ReactNode }) {
  return <div className="ui-split__explain">{children}</div>;
}

export function Panel({ children, sticky = true }: { children: ReactNode; sticky?: boolean }) {
  return <div className={`ui-split__panel${sticky ? ' is-sticky' : ''}`}>{children}</div>;
}
