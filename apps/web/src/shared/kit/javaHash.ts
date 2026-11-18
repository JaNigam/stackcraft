/**
 * Java's HashMap maths, bit for bit, so diagrams show what the JVM really does.
 * Tests pin these against numbers printed by a real JVM run (CollectionsTour.out: "apple" -> 93029210 -> bucket 1).
 */

/** java.lang.String#hashCode: s[0]*31^(n-1) + ... + s[n-1], in 32-bit int arithmetic. */
export function javaStringHash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}

/** HashMap.hash(): mixes the high 16 bits into the low 16 so small tables still use them. */
export function spread(h: number): number {
  return (h ^ (h >>> 16)) | 0;
}

export function bucketIndex(key: string, capacity: number): number {
  return spread(javaStringHash(key)) & (capacity - 1);
}

/** 32-bit binary, grouped in bytes, for chalkboard panels. */
export function bits(n: number): string {
  return (n >>> 0).toString(2).padStart(32, '0').replace(/(.{8})(?=.)/g, '$1 ');
}

export interface AddResult {
  added: boolean;
  bucket: number;
  resized: boolean;
}

/**
 * Model of HashSet/HashMap iteration order: buckets in index order, each chain in insertion order (Java 8+ appends
 * at the tail; resize splits each chain preserving relative order). Resizes at size > 0.75 * capacity, and also when
 * one chain reaches 8 while the table is under 64 (treeifyBin's "resize instead" rule). Treeification itself is
 * not modelled; playgrounds cap input well below it.
 */
export class SimHashSet {
  capacity: number;
  table: string[][];
  size = 0;

  constructor(capacity = 16) {
    this.capacity = capacity;
    this.table = Array.from({ length: capacity }, () => []);
  }

  has(key: string): boolean {
    return this.table[bucketIndex(key, this.capacity)].includes(key);
  }

  add(key: string): AddResult {
    const b = bucketIndex(key, this.capacity);
    if (this.table[b].includes(key)) return { added: false, bucket: b, resized: false };
    this.table[b].push(key);
    let resized = false;
    if (this.table[b].length >= 8 && this.capacity < 64) {
      this.resize();
      resized = true;
    }
    if (++this.size > this.capacity * 0.75) {
      this.resize();
      resized = true;
    }
    return { added: true, bucket: bucketIndex(key, this.capacity), resized };
  }

  private resize() {
    const old = this.table;
    this.capacity *= 2;
    this.table = Array.from({ length: this.capacity }, () => []);
    for (const chain of old) for (const k of chain) this.table[bucketIndex(k, this.capacity)].push(k);
  }

  /** Same order as iterating a java.util.HashSet with the same history. */
  values(): string[] {
    return this.table.flat();
  }
}

/** java.util.AbstractCollection#toString format: [a, b, c] */
export function javaToString(items: readonly unknown[]): string {
  return `[${items.join(', ')}]`;
}
