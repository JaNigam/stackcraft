import { useMemo, type ReactNode } from 'react';

/**
 * Read-only Java highlighting with a tiny tokenizer (keywords, types, strings, numbers, comments). No runtime
 * dependency and no flash of unstyled code; the editable runner uses CodeMirror instead.
 */
const KEYWORDS = new Set(
  'abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while var record sealed permits yield true false null'.split(' '),
);

const TOKEN = /(\/\/[^\n]*|\/\*[\s\S]*?\*\/)|("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])')|(\b\d[\d_]*(?:\.\d+)?[lLfFdD]?\b)|(\b[A-Za-z_$][\w$]*\b)|(@\w+)/g;

export function highlightJava(code: string): ReactNode[] {
  const out: ReactNode[] = [];
  let last = 0;
  let i = 0;
  for (const m of code.matchAll(TOKEN)) {
    const at = m.index ?? 0;
    if (at > last) out.push(code.slice(last, at));
    const [text, comment, str, num, word, anno] = m;
    let cls: string | undefined;
    if (comment) cls = 'tk-c';
    else if (str) cls = 'tk-s';
    else if (num) cls = 'tk-n';
    else if (anno) cls = 'tk-a';
    else if (word) cls = KEYWORDS.has(word) ? 'tk-k' : /^[A-Z]/.test(word) ? 'tk-t' : undefined;
    out.push(cls ? <span key={i++} className={cls}>{text}</span> : text);
    last = at + text.length;
  }
  if (last < code.length) out.push(code.slice(last));
  return out;
}

export function CodeBlock({ code, language = 'java', caption }: { code: string; language?: string; caption?: ReactNode }) {
  const trimmed = code.replace(/\n+$/, '');
  const nodes = useMemo(() => (language === 'java' ? highlightJava(trimmed) : [trimmed]), [trimmed, language]);
  return (
    <figure className="ui-code">
      {caption && <figcaption className="ui-code__caption">{caption}</figcaption>}
      <pre tabIndex={0}>
        <code className={`language-${language}`}>{nodes}</code>
      </pre>
    </figure>
  );
}

/** Output panel in the same chalk style as the runner. */
export function OutputBlock({ text, label = 'Output' }: { text: string; label?: string }) {
  return (
    <figure className="ui-output">
      <figcaption>{label}</figcaption>
      <pre tabIndex={0}>{text.replace(/\n+$/, '')}</pre>
    </figure>
  );
}
