import { useState } from 'react';

export type Family = 'root' | 'list' | 'set' | 'queue' | 'map';

export interface TreeNode {
  label: string;
  kind: 'interface' | 'class';
  family: Family;
  note?: string;
  children?: TreeNode[];
}

const familyBg: Record<Family, string> = {
  root: 'var(--cream)',
  list: 'var(--mint)',
  set: 'var(--lavender)',
  queue: 'var(--sky)',
  map: 'var(--amber)',
};

function Branch({ node, selected, onSelect }: { node: TreeNode; selected?: string; onSelect: (n: TreeNode) => void }) {
  const isOn = selected === node.label;
  return (
    <li className="k-tree__item">
      <button
        type="button"
        className={`k-tree__node k-tree__node--${node.kind}${isOn ? ' is-on' : ''}`}
        style={{ background: familyBg[node.family] }}
        aria-pressed={isOn}
        onClick={() => onSelect(node)}
      >
        {node.kind === 'interface' && <span className="k-tree__tag">interface</span>}
        {node.label}
      </button>
      {node.children && node.children.length > 0 && (
        <ul className="k-tree__children">
          {node.children.map((c) => (
            <Branch key={c.label} node={c} selected={selected} onSelect={onSelect} />
          ))}
        </ul>
      )}
    </li>
  );
}

/**
 * Family tree as an indented outline (works at phone width). Interfaces are dashed sticky notes, classes are
 * solid stickers, colour = family. Tap a node for a one-line fact.
 */
export function HierarchyTree({ roots, caption }: { roots: TreeNode[]; caption?: string }) {
  const [selected, setSelected] = useState<TreeNode | undefined>();
  return (
    <figure className="k-tree">
      <div className="k-tree__roots">
        {roots.map((r) => (
          <ul key={r.label} className="k-tree__root">
            <Branch node={r} selected={selected?.label} onSelect={(n) => setSelected((s) => (s?.label === n.label ? undefined : n))} />
          </ul>
        ))}
      </div>
      <div className="k-tree__fact" aria-live="polite">
        {selected ? (
          <>
            <strong>{selected.label}</strong>: {selected.note ?? (selected.kind === 'interface' ? 'An interface: a contract, no storage.' : 'A concrete class you can new up.')}
          </>
        ) : (
          <span className="k-muted">Tap any sticker for its one-line fact.</span>
        )}
      </div>
      {caption && <figcaption className="k-caption">{caption}</figcaption>}
    </figure>
  );
}
