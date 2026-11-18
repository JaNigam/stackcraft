import { describe, expect, it } from 'vitest';
import { SimHashSet, bucketIndex, javaStringHash, javaToString } from './javaHash';

// Expected values were printed by a real JVM (content/.../code/CollectionsTour.out and a probe run), not computed here.
describe('Java hash maths', () => {
  it('matches String.hashCode from the JVM', () => {
    expect(javaStringHash('apple')).toBe(93029210);
    expect(javaStringHash('')).toBe(0);
  });

  it('puts the tour words in the same buckets as HashMap', () => {
    expect(bucketIndex('apple', 16)).toBe(1);
    expect(bucketIndex('banana', 16)).toBe(0);
    expect(bucketIndex('kiwi', 16)).toBe(2);
    expect(bucketIndex('mango', 16)).toBe(15);
    expect(bucketIndex('cherry', 16)).toBe(1);
  });

  it('iterates like java.util.HashSet', () => {
    const s = new SimHashSet();
    for (const w of ['mango', 'kiwi', 'apple', 'banana', 'apple']) s.add(w);
    expect(javaToString(s.values())).toBe('[banana, apple, kiwi, mango]'); // CollectionsTour.out line 2
    expect(s.size).toBe(4);
  });

  it('rejects duplicates and resizes past the 0.75 load factor', () => {
    const s = new SimHashSet();
    expect(s.add('apple').added).toBe(true);
    expect(s.add('apple').added).toBe(false);
    for (let i = 0; i < 12; i++) s.add(`w${i}`);
    expect(s.capacity).toBe(32); // 13 entries > 12 = 16 * 0.75
    expect(s.values()).toHaveLength(13);
    expect(new Set(s.values()).size).toBe(13);
  });
});
