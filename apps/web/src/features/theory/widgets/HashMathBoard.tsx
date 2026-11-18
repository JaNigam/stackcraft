import { useState } from 'react';
import { Chalk, Chalkboard, bits, javaStringHash, spread } from '@/shared/kit';
import { Button } from '@/shared/ui';

/**
 * Exact internals, so it goes on the chalkboard: the real field layout of HashMap and the three-step maths that
 * picks a bucket. Numbers are computed with Java's own algorithm (unit-tested against a JVM run).
 */
export function HashMathBoard({ initial = 'apple' }: { initial?: string }) {
  const [word, setWord] = useState(initial);
  const [cap, setCap] = useState(16);
  const h = javaStringHash(word);
  const hi = h >>> 16;
  const s = spread(h);
  const b = s & (cap - 1);
  return (
    <Chalkboard title="Inside java.util.HashMap">
      <pre>
        <Chalk color="mint">transient</Chalk> Node&lt;K,V&gt;[] <Chalk color="yellow">table</Chalk>;{'   '}
        <Chalk color="pink">// length is always a power of two</Chalk>
        {'\n\n'}
        <Chalk color="mint">static class</Chalk> Node&lt;K,V&gt; {'{'}
        {'\n'}    <Chalk color="mint">final int</Chalk> hash;{'    '}<Chalk color="pink">// spread hash, cached</Chalk>
        {'\n'}    <Chalk color="mint">final</Chalk> K key;
        {'\n'}    V value;
        {'\n'}    Node&lt;K,V&gt; next;{'    '}<Chalk color="pink">// the chain inside one bucket</Chalk>
        {'\n'}
        {'}'}
      </pre>
      <div className="hm-controls">
        <label>
          key{' '}
          <input className="hm-input" value={word} maxLength={16} onChange={(e) => setWord(e.target.value)} aria-label="Key to hash" />
        </label>
        <span role="group" aria-label="Table length">
          {[16, 32, 64].map((c) => (
            <Button key={c} className={`hm-cap${cap === c ? ' is-picked' : ''}`} onClick={() => setCap(c)} aria-pressed={cap === c}>
              {c}
            </Button>
          ))}
        </span>
      </div>
      <pre className="hm-steps">
        <Chalk color="yellow">1</Chalk> h = "{word}".hashCode(){'      '}= {h}
        {'\n'}                                 {bits(h)}
        {'\n'}
        <Chalk color="yellow">2</Chalk> h ^ (h &gt;&gt;&gt; 16){'              '}h&gt;&gt;&gt;16 = {hi}
        {'\n'}                                 {bits(s)}  = {s}
        {'\n'}
        <Chalk color="yellow">3</Chalk> (n - 1) &amp; hash, n = {cap}{'         '}{cap - 1} = {bits(cap - 1).slice(-9)}
        {'\n'}   <Chalk color="mint">bucket = {b}</Chalk>
      </pre>
    </Chalkboard>
  );
}
