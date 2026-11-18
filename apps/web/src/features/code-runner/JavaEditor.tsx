import CodeMirror from '@uiw/react-codemirror';
import { java } from '@codemirror/lang-java';

/** Lazy-loaded so CodeMirror is only downloaded when someone actually edits code. */
export default function JavaEditor({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const dark = document.documentElement.dataset.theme === 'dark' || (!document.documentElement.dataset.theme && matchMedia('(prefers-color-scheme: dark)').matches);
  return (
    <div className="cr__editor">
      <CodeMirror value={value} onChange={onChange} extensions={[java()]} theme={dark ? 'dark' : 'light'} basicSetup={{ foldGutter: false }} maxHeight="58vh" aria-label="Java editor" />
    </div>
  );
}
