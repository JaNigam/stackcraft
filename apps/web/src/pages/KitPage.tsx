import { Actor, Bucket, Chain, Chalk, Chalkboard, Character, HeapBlob, Machine, NodeBox, Playground, Scene, StackFrame, Stepper, bucketIndex, javaStringHash, type CharacterKind, type Mood } from '@/shared/kit';
import { Badge, Button, Callout, Card } from '@/shared/ui';
import { CodeBlock, OutputBlock } from '@/shared/ui/CodeBlock';
import { CollectionsFamilyTree } from '@/features/theory/widgets/CollectionsFamilyTree';
import { HashMathBoard } from '@/features/theory/widgets/HashMathBoard';
import { useState } from 'react';

const KINDS: CharacterKind[] = ['list', 'set', 'map', 'queue', 'tree', 'blob'];
const MOODS: Mood[] = ['happy', 'smug', 'surprised', 'sad', 'nope', 'sleepy'];

/** Showcase of the locked A + D kit. Every chapter is built from these pieces (docs/STYLE-GUIDE.md). */
export function KitPage() {
  const [mood, setMood] = useState<Mood>('happy');
  return (
    <div className="kitpage">
      <h1>Diagram kit</h1>
      <p>The locked look: sticker-doodle characters for intuition, playgrounds for anything you can operate, chalkboards only for exact internals.</p>

      <h2>Characters</h2>
      <div className="qz-row" role="group" aria-label="Mood">
        {MOODS.map((m) => (
          <Button key={m} className={m === mood ? 'is-picked' : ''} onClick={() => setMood(m)} aria-pressed={m === mood}>
            {m}
          </Button>
        ))}
      </div>
      <div className="kit-row">
        {KINDS.map((k) => (
          <Character key={k} kind={k} mood={mood} label={k} bump={mood} />
        ))}
      </div>

      <h2>Scene (analogy strip)</h2>
      <Scene>
        <Actor kind="queue" label="ArrayDeque" says="First in, first out. Or last in, first out. Your call." />
        <Actor kind="tree" mood="smug" label="TreeSet" says="I keep everyone sorted." />
      </Scene>

      <h2>Buckets and chains</h2>
      <Card>
        <div className="kit-row kit-row--tight">
          {Array.from({ length: 8 }, (_, i) => (
            <Bucket key={i} index={i} highlight={i === bucketIndex('apple', 8)}>
              {['apple', 'kiwi', 'mango', 'banana', 'cherry'].filter((w) => bucketIndex(w, 8) === i).map((w) => (
                <span key={w} className="k-pill">
                  {w}
                </span>
              ))}
            </Bucket>
          ))}
        </div>
        <p className="k-caption">8 buckets, keys placed with Java's real hash ("apple".hashCode() = {javaStringHash('apple')}).</p>
      </Card>
      <div className="kit-gap" />
      <Chain>
        {[
          <NodeBox key="a" title="Node" fields={[{ name: 'key', value: '"apple"' }, { name: 'value', value: '25' }]} />,
          <NodeBox key="c" title="Node" color="var(--mint)" fields={[{ name: 'key', value: '"cherry"' }, { name: 'value', value: '7' }]} />,
        ]}
      </Chain>

      <h2>Memory</h2>
      <div className="kit-row">
        <StackFrame method="main(String[])" active locals={[{ name: 'n', value: '3' }, { name: 'list', value: 'ArrayList', ref: true }]} />
        <HeapBlob type="ArrayList" fields={[{ name: 'size', value: '3' }, { name: 'elementData', value: 'Object[10]' }]} />
      </div>
      <Machine label="hash → spread → mask" input={'"apple"'} output="bucket 1" />

      <h2>Chalkboard (exact internals only)</h2>
      <HashMathBoard />
      <Chalkboard title="Plain board">
        <pre>
          <Chalk color="yellow">ArrayList</Chalk>.elementData = Object[<Chalk color="mint">10</Chalk>]{'\n'}grow: newCapacity = old + (old &gt;&gt; 1)
        </pre>
      </Chalkboard>

      <h2>Stepper</h2>
      <Stepper
        steps={[
          { title: 'Compute hashCode', body: <p>"apple".hashCode() runs the 31-polynomial over the characters.</p> },
          { title: 'Spread', body: <p>XOR the high 16 bits into the low 16 so small tables still feel them.</p> },
          { title: 'Mask', body: <p>(n − 1) &amp; hash picks the bucket because n is a power of two.</p> },
        ]}
      />

      <h2>Playground frame</h2>
      <Playground title="Any interactive widget" hint="Controls on top, stage in the middle, live readout at the bottom." controls={<Button variant="primary">A control</Button>} readout={'readout: values update here (aria-live)'}>
        <p>Stage</p>
      </Playground>

      <h2>Family tree</h2>
      <CollectionsFamilyTree />

      <h2>Text pieces</h2>
      <Callout kind="trap">Printing a PriorityQueue does not show sorted order.</Callout>
      <Callout kind="java">Java nudge: List.of is immutable and rejects null.</Callout>
      <Callout kind="spring">Spring nudge: Spring injects a List of every bean of a type: that is how the grader registry works.</Callout>
      <div className="qz-row">
        {(['mint', 'lavender', 'pink', 'amber', 'sky', 'ok', 'bad'] as const).map((t) => (
          <Badge key={t} tone={t}>
            {t}
          </Badge>
        ))}
      </div>
      <CodeBlock code={'Map<String, Integer> stock = new HashMap<>();\nstock.put("apple", 10); // returns null'} />
      <OutputBlock text={'6 put first      -> null\n7 put again      -> 10'} />
    </div>
  );
}
