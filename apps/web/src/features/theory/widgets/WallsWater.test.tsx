import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { codeFile } from '@/features/code-runner/codeFiles';
import { containerFrames, containerTraceLine } from './containerFrames';
import { rainFrames, rainTraceLine } from './rainFrames';
import { ContainerPlayground } from './ContainerPlayground';
import { RainWaterPlayground } from './RainWaterPlayground';
import { parseIntList } from './stepper';

/** Lines of a verified .out between two markers (Windows line endings tolerated). */
function between(chapterId: string, file: string, start: string, end: string): string[] {
  const lines = codeFile(chapterId, file)!.verifiedOutput!.split(/\r?\n/);
  return lines.slice(lines.indexOf(start) + 1, lines.indexOf(end));
}

describe('containerFrames', () => {
  it('prints exactly the trace ContainerWithMostWater.java printed', () => {
    const java = between('dsa.u01.l02', 'ContainerWithMostWater.java', '== trace for [1, 8, 6, 2, 5, 4, 8, 3, 7]', 'result 49');
    expect(java).toHaveLength(8);
    const ours = containerFrames([1, 8, 6, 2, 5, 4, 8, 3, 7]).map(containerTraceLine).filter((l): l is string => l !== null);
    expect(ours).toEqual(java);
  });

  it('finds the same best as the Java run, and remembers which walls', () => {
    const last = (h: number[]) => containerFrames(h).at(-1)!;
    expect(last([1, 8, 6, 2, 5, 4, 8, 3, 7])).toMatchObject({ best: 49, bestPair: [1, 8] });
    expect(last([4, 3, 2, 1, 4]).best).toBe(16);
    expect(last([5, 1, 1, 1, 1, 9, 4]).best).toBe(25);
    expect(last([7]).best).toBe(0);
    expect(last([]).best).toBe(0);
  });
});

describe('rainFrames', () => {
  it('prints exactly the two-pointer trace TrappingRainWater.java printed', () => {
    const java = between('dsa.u01.l03', 'TrappingRainWater.java', '== two-pointer trace for [0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]', 'result 6');
    expect(java).toHaveLength(11);
    const ours = rainFrames([0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]).map(rainTraceLine).filter((l): l is string => l !== null);
    expect(ours).toEqual(java);
  });

  it('ends with the same per-column water as the prefix-max version printed', () => {
    const verified = codeFile('dsa.u01.l03', 'TrappingRainWater.java')!.verifiedOutput!.split(/\r?\n/);
    const waterLine = verified.find((l) => l.startsWith('water    '))!;
    expect(`water    [${rainFrames([0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]).at(-1)!.water.join(', ')}]`).toBe(waterLine);
  });

  it('matches the other Java results', () => {
    const total = (h: number[]) => rainFrames(h).at(-1)!.total;
    expect(total([4, 2, 0, 3, 2, 5])).toBe(9);
    expect(total([3, 0, 2, 0, 4])).toBe(7);
    expect(total([1, 2, 3, 4])).toBe(0);
    expect(total([5, 0, 5])).toBe(5);
    expect(total([2])).toBe(0);
    expect(total([])).toBe(0);
  });
});

describe('playgrounds', () => {
  it('Container: steps show the Java trace line and the best area', async () => {
    render(<ContainerPlayground />);
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('L=1(8) R=8(7) level=7 width=7 area=49 best=49 -> right-- (right is shorter)')).toBeInTheDocument();
  });

  it('Rain water: steps pour water and show the Java trace line', async () => {
    render(<RainWaterPlayground />);
    for (let k = 0; k < 4; k++) await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('L=2(0) leftMax=1 -> water 1, total=1, left++')).toBeInTheDocument();
  });

  it('rejects out-of-range input', async () => {
    render(<RainWaterPlayground />);
    const input = screen.getByLabelText('Bar heights');
    await userEvent.clear(input);
    await userEvent.type(input, '1, 99');
    await userEvent.click(screen.getByRole('button', { name: 'Use these' }));
    expect(screen.getByRole('alert')).toHaveTextContent('0 to 12');
    expect(parseIntList('1 2,3', { min: 0, max: 5, maxLen: 4 })).toEqual([1, 2, 3]);
  });
});
