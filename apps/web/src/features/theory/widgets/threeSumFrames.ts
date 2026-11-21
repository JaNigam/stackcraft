/**
 * The 3Sum algorithm as a list of frames, one per decision, so the playground can step forwards and backwards.
 * It mirrors code/ThreeSum.java exactly; `traceLine` prints each frame the way the Java trace does, and a test
 * compares the two line by line against the verified ThreeSum.out.
 */
export type FrameKind = 'sorted' | 'captain' | 'skip-captain' | 'stop-positive' | 'too-small' | 'too-big' | 'found' | 'done';

export interface Frame {
  kind: FrameKind;
  sorted: number[];
  i: number;
  left: number;
  right: number;
  sum: number | null;
  /** Triplets found up to and including this frame. */
  found: number[][];
  /** Sum checks done so far (the two-pointer comparisons). */
  checks: number;
}

export function threeSumFrames(input: number[]): Frame[] {
  const a = [...input].sort((x, y) => x - y);
  const frames: Frame[] = [];
  const found: number[][] = [];
  let checks = 0;
  const push = (kind: FrameKind, i: number, left: number, right: number, sum: number | null) =>
    frames.push({ kind, sorted: a, i, left, right, sum, found: found.map((t) => [...t]), checks });

  push('sorted', -1, -1, -1, null);
  for (let i = 0; i < a.length - 2; i++) {
    if (a[i] > 0) {
      push('stop-positive', i, -1, -1, null);
      break;
    }
    if (i > 0 && a[i] === a[i - 1]) {
      push('skip-captain', i, -1, -1, null);
      continue;
    }
    push('captain', i, -1, -1, null);
    let left = i + 1;
    let right = a.length - 1;
    while (left < right) {
      const sum = a[i] + a[left] + a[right];
      checks++;
      if (sum === 0) {
        found.push([a[i], a[left], a[right]]);
        push('found', i, left, right, sum);
        left++;
        right--;
        while (left < right && a[left] === a[left - 1]) left++;
        while (left < right && a[right] === a[right + 1]) right--;
      } else if (sum < 0) {
        push('too-small', i, left, right, sum);
        left++;
      } else {
        push('too-big', i, left, right, sum);
        right--;
      }
    }
  }
  push('done', -1, -1, -1, null);
  return frames;
}

const list = (t: number[]) => `[${t.join(', ')}]`;

/** Same text as ThreeSum.java's trace (null for frames the Java trace does not print). */
export function traceLine(f: Frame): string | null {
  const at = () => `  L=${f.left}(${f.sorted[f.left]}) R=${f.right}(${f.sorted[f.right]}) sum=${f.sum}`;
  switch (f.kind) {
    case 'sorted':
      return `sorted ${list(f.sorted)}`;
    case 'captain':
      return `captain i=${f.i} (${f.sorted[f.i]})`;
    case 'skip-captain':
      return `captain i=${f.i} (${f.sorted[f.i]}) same as previous captain -> skip`;
    case 'stop-positive':
      return `captain i=${f.i} (${f.sorted[f.i]}) is positive -> stop`;
    case 'found':
      return `${at()} -> found ${list(f.found[f.found.length - 1])}`;
    case 'too-small':
      return `${at()} -> too small, left++`;
    case 'too-big':
      return `${at()} -> too big, right--`;
    case 'done':
      return null;
  }
}

/** Plain-language explanation of a frame for the readout. */
export function explain(f: Frame): string {
  const n = f.sorted.length;
  switch (f.kind) {
    case 'sorted':
      return n < 3 ? 'Fewer than 3 numbers: no triplet is possible.' : 'Sorted first, so "too small" and "too big" tell us which pointer to move.';
    case 'captain':
      return `Captain ${f.sorted[f.i]}: now find two numbers to its right that add up to ${-f.sorted[f.i]} (Two Sum II).`;
    case 'skip-captain':
      return `Captain ${f.sorted[f.i]} again: every team it could make was already found, so skip it.`;
    case 'stop-positive':
      return `Captain ${f.sorted[f.i]} is positive and everything after it is bigger, so no sum can reach 0. Stop.`;
    case 'found':
      return 'Sum is 0: record the team, move both pointers, and skip equal neighbours so it is not recorded twice.';
    case 'too-small':
      return `Sum ${f.sum} is too small: only a bigger left number can help, so left moves right.`;
    case 'too-big':
      return `Sum ${f.sum} is too big: only a smaller right number can help, so right moves left.`;
    case 'done':
      return `Done: ${f.found.length} unique triplet${f.found.length === 1 ? '' : 's'} after ${f.checks} sum check${f.checks === 1 ? '' : 's'}.`;
  }
}

/** n choose 3: how many triples brute force would try. */
export function bruteForceChecks(n: number): number {
  return n < 3 ? 0 : (n * (n - 1) * (n - 2)) / 6;
}
