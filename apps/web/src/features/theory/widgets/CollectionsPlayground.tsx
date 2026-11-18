import { useMemo, useState, type FormEvent } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { Bucket, Chain, Character, Playground, SimHashSet, bucketIndex, javaStringHash, javaToString, spread, type Mood } from '@/shared/kit';
import { Button } from '@/shared/ui';

const PRESETS = ['mango', 'kiwi', 'apple', 'banana', 'cherry', 'fig'];
const TOUR = ['mango', 'kiwi', 'apple', 'banana', 'apple'];
const MAX = 24;

/** Replays the add() history through each collection's real rules. HashSet order comes from Java's own hash maths. */
export function simulate(words: string[]) {
  const hash = new SimHashSet();
  const linked: string[] = [];
  const results: boolean[] = [];
  for (const w of words) {
    results.push(hash.add(w).added);
    if (!linked.includes(w)) linked.push(w);
  }
  const tree = [...linked].sort(); // String.compareTo == UTF-16 code unit order == JS default sort
  return { list: [...words], hash, linked, tree, results };
}

function Pill({ word, isNew, rejected }: { word: string; isNew?: boolean; rejected?: boolean }) {
  const reduce = useReducedMotion();
  return (
    <motion.span
      className={`k-pill${isNew ? ' is-new' : ''}${rejected ? ' is-rejected' : ''}`}
      initial={isNew && !reduce ? { y: -26, scale: 0.7, opacity: 0 } : false}
      animate={{ y: 0, scale: 1, opacity: 1 }}
      transition={{ type: 'spring', stiffness: 520, damping: 17 }}
    >
      {word}
    </motion.span>
  );
}

function Column({ title, kind, mood, bump, printed, children }: { title: string; kind: 'list' | 'set' | 'tree'; mood: Mood; bump: number; printed: string; children: React.ReactNode }) {
  return (
    <div className={`cp-col cp-col--${title.toLowerCase()}`}>
      <Character kind={kind === 'tree' ? 'tree' : kind} mood={mood} label={title} size={76} bump={bump} />
      <div className="cp-col__body">{children}</div>
      <code className="cp-col__print">{printed}</code>
    </div>
  );
}

export function CollectionsPlayground() {
  const [words, setWords] = useState<string[]>([]);
  const [draft, setDraft] = useState('');
  const sim = useMemo(() => simulate(words), [words]);
  const last = words[words.length - 1];
  const lastAdded = sim.results[sim.results.length - 1];
  const n = words.length;

  const add = (w: string) => {
    const word = w.trim().toLowerCase().replace(/[^a-z]/g, '').slice(0, 12);
    if (!word || n >= MAX) return;
    setWords((ws) => [...ws, word]);
  };
  const submit = (e: FormEvent) => {
    e.preventDefault();
    add(draft);
    setDraft('');
  };

  const h = last ? javaStringHash(last) : 0;
  const readout = last
    ? `add("${last}") -> ArrayList true, HashSet ${lastAdded}, LinkedHashSet ${lastAdded}, TreeSet ${lastAdded}\n` +
      `"${last}".hashCode() = ${h}   spread = ${spread(h)}   & ${sim.hash.capacity - 1} = bucket ${bucketIndex(last, sim.hash.capacity)} of ${sim.hash.capacity}`
    : 'Add a word. Watch where it lands in each collection.';

  return (
    <Playground
      title="Four collections, same add() calls"
      hint="Every word goes into all four at once. Try a duplicate, then try the tour's words."
      controls={
        <>
          <form onSubmit={submit} className="cp-form">
            <label className="sr-only" htmlFor="cp-word">
              Word to add
            </label>
            <input id="cp-word" className="ui-input" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="type a fruit" maxLength={12} autoComplete="off" />
            <Button type="submit" variant="primary" disabled={!draft.trim() || n >= MAX}>
              add()
            </Button>
          </form>
          <div className="cp-chips">
            {PRESETS.map((p) => (
              <Button key={p} onClick={() => add(p)} disabled={n >= MAX}>
                {p}
              </Button>
            ))}
          </div>
          <Button onClick={() => setWords(TOUR)}>Load the tour</Button>
          <Button variant="ghost" onClick={() => setWords([])} disabled={n === 0}>
            Reset
          </Button>
        </>
      }
      readout={readout}
    >
      <div className="cp-grid">
        <Column title="ArrayList" kind="list" mood="happy" bump={n} printed={javaToString(sim.list)}>
          <ol className="cp-slots">
            {sim.list.map((w, i) => (
              <li key={i}>
                <span className="cp-slots__i">{i}</span>
                <Pill word={w} isNew={i === n - 1} />
              </li>
            ))}
          </ol>
        </Column>

        <Column title="HashSet" kind="set" mood={last && !lastAdded ? 'nope' : 'happy'} bump={n} printed={javaToString(sim.hash.values())}>
          <div className={`cp-buckets cp-buckets--${sim.hash.capacity}`}>
            {sim.hash.table.map((chain, b) => (
              <Bucket key={b} index={b} compact highlight={!!last && b === bucketIndex(last, sim.hash.capacity)}>
                {chain.map((w) => (
                  <Pill key={w} word={w} isNew={w === last && lastAdded} />
                ))}
              </Bucket>
            ))}
          </div>
          {last && !lastAdded && <p className="cp-nope">"{last}" is already in bucket {bucketIndex(last, sim.hash.capacity)}: add() returns false.</p>}
        </Column>

        <Column title="LinkedHashSet" kind="set" mood="happy" bump={n} printed={javaToString(sim.linked)}>
          {sim.linked.length > 0 ? (
            <Chain terminated={false}>
              {sim.linked.map((w) => (
                <Pill key={w} word={w} isNew={w === last && lastAdded} />
              ))}
            </Chain>
          ) : (
            <span className="k-muted">empty</span>
          )}
        </Column>

        <Column title="TreeSet" kind="tree" mood="smug" bump={n} printed={javaToString(sim.tree)}>
          <div className="cp-shelf">
            {sim.tree.map((w) => (
              <Pill key={w} word={w} isNew={w === last && lastAdded} />
            ))}
          </div>
        </Column>
      </div>
    </Playground>
  );
}
