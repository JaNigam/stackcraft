/**
 * Trapping Rain Water (two pointers) as frames, one per settled column. Mirrors twoPointers() in
 * code/TrappingRainWater.java; rainTraceLine prints the same text, and a test compares them line by line.
 */
export type RainKind = 'start' | 'new-left-max' | 'water-left' | 'new-right-max' | 'water-right' | 'done';

export interface RainFrame {
  kind: RainKind;
  heights: number[];
  /** The column settled in this frame (-1 for start/done). */
  at: number;
  left: number;
  right: number;
  leftMax: number;
  rightMax: number;
  total: number;
  /** Water per column settled so far. */
  water: number[];
  /** Columns settled so far. */
  done: boolean[];
}

export function rainFrames(heights: number[]): RainFrame[] {
  const n = heights.length;
  const water = new Array<number>(n).fill(0);
  const done = new Array<boolean>(n).fill(false);
  const frames: RainFrame[] = [];
  let left = 0;
  let right = n - 1;
  let leftMax = 0;
  let rightMax = 0;
  let total = 0;
  const push = (kind: RainKind, at: number) =>
    frames.push({ kind, heights, at, left, right, leftMax, rightMax, total, water: [...water], done: [...done] });

  push('start', -1);
  while (left < right) {
    if (heights[left] < heights[right]) {
      if (heights[left] >= leftMax) {
        leftMax = heights[left];
        done[left] = true;
        push('new-left-max', left);
      } else {
        water[left] = leftMax - heights[left];
        total += water[left];
        done[left] = true;
        push('water-left', left);
      }
      left++;
    } else {
      if (heights[right] >= rightMax) {
        rightMax = heights[right];
        done[right] = true;
        push('new-right-max', right);
      } else {
        water[right] = rightMax - heights[right];
        total += water[right];
        done[right] = true;
        push('water-right', right);
      }
      right--;
    }
  }
  if (n > 0) done[left] = true; // where the pointers meet: the tallest bar, never holds water
  push('done', -1);
  return frames;
}

/** Same text as TrappingRainWater.java's two-pointer trace (null for start/done). */
export function rainTraceLine(f: RainFrame): string | null {
  const h = f.heights[f.at];
  switch (f.kind) {
    case 'new-left-max':
      return `  L=${f.at}(${h}) new leftMax=${f.leftMax} -> no water, left++`;
    case 'water-left':
      return `  L=${f.at}(${h}) leftMax=${f.leftMax} -> water ${f.water[f.at]}, total=${f.total}, left++`;
    case 'new-right-max':
      return `  R=${f.at}(${h}) new rightMax=${f.rightMax} -> no water, right--`;
    case 'water-right':
      return `  R=${f.at}(${h}) rightMax=${f.rightMax} -> water ${f.water[f.at]}, total=${f.total}, right--`;
    default:
      return null;
  }
}

export function rainExplain(f: RainFrame): string {
  const h = f.heights[f.at];
  switch (f.kind) {
    case 'start':
      return f.heights.length < 3 ? 'Fewer than three bars: water needs a wall on both sides, so the answer is 0.' : 'Two pointers at the ends. Each step settles the side whose wall is lower.';
    case 'new-left-max':
      return `Left bar ${h} is the tallest so far on the left: nothing holds water above it. It becomes leftMax.`;
    case 'water-left':
      return `Left is the lower side, so its water is decided by leftMax (${f.leftMax}): the right side has a wall at least as tall. ${f.leftMax} - ${h} = ${f.water[f.at]}.`;
    case 'new-right-max':
      return `Right bar ${h} is the tallest so far on the right: nothing holds water above it. It becomes rightMax.`;
    case 'water-right':
      return `Right is the lower side (or a tie), so its water is decided by rightMax (${f.rightMax}). ${f.rightMax} - ${h} = ${f.water[f.at]}.`;
    case 'done':
      return `Pointers met at the tallest bar. Total water: ${f.total}.`;
  }
}
