/**
 * Container With Most Water as frames, one per measured container. Mirrors code/ContainerWithMostWater.java;
 * traceLine prints the same text as its trace, and a test compares them line by line.
 */
export interface ContainerFrame {
  kind: 'start' | 'measure' | 'done';
  heights: number[];
  left: number;
  right: number;
  level: number;
  width: number;
  area: number;
  best: number;
  /** The pair that produced `best` so far. */
  bestPair: [number, number] | null;
  move: 'left' | 'right' | 'tie' | null;
}

export function containerFrames(heights: number[]): ContainerFrame[] {
  const frames: ContainerFrame[] = [];
  let left = 0;
  let right = heights.length - 1;
  let best = 0;
  let bestPair: [number, number] | null = null;
  frames.push({ kind: 'start', heights, left, right, level: 0, width: 0, area: 0, best, bestPair, move: null });
  while (left < right) {
    const level = Math.min(heights[left], heights[right]);
    const width = right - left;
    const area = level * width;
    if (area > best) {
      best = area;
      bestPair = [left, right];
    }
    const move = heights[left] < heights[right] ? 'left' : heights[left] > heights[right] ? 'right' : 'tie';
    frames.push({ kind: 'measure', heights, left, right, level, width, area, best, bestPair, move });
    if (heights[left] < heights[right]) left++;
    else right--;
  }
  frames.push({ kind: 'done', heights, left: -1, right: -1, level: 0, width: 0, area: 0, best, bestPair, move: null });
  return frames;
}

/** Same text as ContainerWithMostWater.java's trace (null for frames it does not print). */
export function containerTraceLine(f: ContainerFrame): string | null {
  if (f.kind !== 'measure') return null;
  const move = f.move === 'left' ? 'left++ (left is shorter)' : f.move === 'right' ? 'right-- (right is shorter)' : 'right-- (tie)';
  return `  L=${f.left}(${f.heights[f.left]}) R=${f.right}(${f.heights[f.right]}) level=${f.level} width=${f.width} area=${f.area} best=${f.best} -> ${move}`;
}

export function containerExplain(f: ContainerFrame): string {
  if (f.kind === 'start') {
    return f.heights.length < 2 ? 'Fewer than two walls: no container, the answer is 0.' : 'Start with the widest container: one wall at each end.';
  }
  if (f.kind === 'done') return `Pointers met. Best container holds ${f.best}.`;
  const shorter = f.move === 'left' ? 'left' : f.move === 'right' ? 'right' : 'either';
  const why =
    f.move === 'tie'
      ? 'Equal walls: any narrower container keeping either one is still capped at this level, so moving either is safe.'
      : `The ${shorter} wall is shorter. Every narrower container that keeps it is capped at ${f.level} and narrower, so it can never beat ${f.area}. Discard it.`;
  return `Water level ${f.level} (the shorter wall) × width ${f.width} = ${f.area}. ${why}`;
}
