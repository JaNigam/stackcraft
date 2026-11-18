import { useState } from 'react';
import { Character, Playground, type CharacterKind } from '@/shared/kit';
import { Button } from '@/shared/ui';

interface Pick {
  cls: string;
  kind: CharacterKind;
  why: string;
  cost: string;
}

interface Ask {
  q: string;
  options: { label: string; next: Ask | Pick }[];
}

const isPick = (n: Ask | Pick): n is Pick => 'cls' in n;

const TREE: Ask = {
  q: 'Do you look things up by a key (id → customer, symbol → price)?',
  options: [
    {
      label: 'Yes, key → value',
      next: {
        q: 'What order do you need when you iterate the keys?',
        options: [
          { label: 'Any order is fine', next: { cls: 'HashMap', kind: 'map', why: 'Fastest general-purpose map.', cost: 'get/put O(1) average' } },
          { label: 'Insertion order (or LRU)', next: { cls: 'LinkedHashMap', kind: 'map', why: 'HashMap speed plus a predictable order; accessOrder=true gives LRU.', cost: 'get/put O(1), a bit more memory' } },
          { label: 'Sorted keys / ranges', next: { cls: 'TreeMap', kind: 'map', why: 'Sorted keys with floorKey, ceilingKey, subMap.', cost: 'get/put O(log n)' } },
        ],
      },
    },
    {
      label: 'No, just elements',
      next: {
        q: 'Must every element be unique?',
        options: [
          {
            label: 'Yes, no duplicates',
            next: {
              q: 'What order do you need?',
              options: [
                { label: 'Any order', next: { cls: 'HashSet', kind: 'set', why: 'O(1) contains is the whole point.', cost: 'add/contains O(1) average' } },
                { label: 'Insertion order', next: { cls: 'LinkedHashSet', kind: 'set', why: 'Dedupe and remember who came first.', cost: 'add/contains O(1)' } },
                { label: 'Sorted', next: { cls: 'TreeSet', kind: 'tree', why: 'Always sorted, plus first(), floor(), headSet().', cost: 'add/contains O(log n)' } },
              ],
            },
          },
          {
            label: 'Duplicates are fine',
            next: {
              q: 'How will you take elements out?',
              options: [
                { label: 'By index, or just iterate', next: { cls: 'ArrayList', kind: 'list', why: 'Contiguous array: cache-friendly and O(1) get(i).', cost: 'get O(1), add at end amortised O(1), insert in middle O(n)' } },
                { label: 'From the ends (FIFO or LIFO)', next: { cls: 'ArrayDeque', kind: 'queue', why: 'The modern stack and queue. Not Stack, not LinkedList.', cost: 'push/pop/offer/poll O(1)' } },
                { label: 'Smallest / most urgent first', next: { cls: 'PriorityQueue', kind: 'queue', why: 'A binary heap: the head is always the minimum.', cost: 'offer/poll O(log n), peek O(1)' } },
              ],
            },
          },
        ],
      },
    },
  ],
};

/** "Which collection?" in three questions: the interview answer as a decision tree. */
export function CollectionPicker() {
  const [path, setPath] = useState<{ ask: Ask; chose: string }[]>([]);
  const [node, setNode] = useState<Ask | Pick>(TREE);

  const choose = (ask: Ask, label: string, next: Ask | Pick) => {
    setPath((p) => [...p, { ask, chose: label }]);
    setNode(next);
  };
  const restart = () => {
    setPath([]);
    setNode(TREE);
  };

  return (
    <Playground title="Pick the right collection in 10 seconds" hint="Answer like you would in the interview. Three questions at most.">
      {path.length > 0 && (
        <ol className="pk-path">
          {path.map((p) => (
            <li key={p.ask.q}>
              <span className="k-muted">{p.ask.q}</span> <strong>{p.chose}</strong>
            </li>
          ))}
        </ol>
      )}
      {isPick(node) ? (
        <div className="pk-result">
          <Character kind={node.kind} mood="happy" size={88} bump={node.cls} />
          <div>
            <div className="pk-result__cls">{node.cls}</div>
            <p>{node.why}</p>
            <p className="pk-result__cost">{node.cost}</p>
            <Button onClick={restart}>Start over</Button>
          </div>
        </div>
      ) : (
        <div className="pk-ask">
          <p className="pk-ask__q">{node.q}</p>
          <div className="qz-row">
            {node.options.map((o) => (
              <Button key={o.label} onClick={() => choose(node, o.label, o.next)}>
                {o.label}
              </Button>
            ))}
          </div>
          {path.length > 0 && (
            <Button variant="ghost" onClick={restart}>
              Start over
            </Button>
          )}
        </div>
      )}
    </Playground>
  );
}
