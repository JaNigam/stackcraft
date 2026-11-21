import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { codeFile } from '@/features/code-runner/codeFiles';
import { bruteForceChecks, threeSumFrames, traceLine } from './threeSumFrames';
import { ThreeSumPlayground, parseNumbers } from './ThreeSumPlayground';

// The .out may have Windows line endings (it is the JVM's real stdout), so split on \r?\n.
const verifiedLines = codeFile('dsa.u01.l01', 'ThreeSum.java')!.verifiedOutput!.split(/\r?\n/);

describe('threeSumFrames', () => {
  it('prints exactly the trace ThreeSum.java printed (verified .out)', () => {
    const javaTrace = verifiedLines.slice(1, verifiedLines.indexOf('result [[-1, -1, 2], [-1, 0, 1]]'));
    expect(javaTrace.length).toBeGreaterThan(5);
    const ours = threeSumFrames([-1, 0, 1, 2, -1, -4]).map(traceLine).filter((l): l is string => l !== null);
    expect(ours).toEqual(javaTrace);
  });

  it('finds each unique triplet once, like the Java run', () => {
    const last = (nums: number[]) => threeSumFrames(nums).at(-1)!.found;
    expect(last([0, 0, 0, 0])).toEqual([[0, 0, 0]]);
    expect(last([-2, 0, 0, 2, 2])).toEqual([[-2, 0, 2]]);
    expect(last([0, 1, 1])).toEqual([]);
    expect(last([])).toEqual([]);
  });

  it('needs 7 sum checks on the example, against 20 brute-force triples (quoted in the theory)', () => {
    expect(threeSumFrames([-1, 0, 1, 2, -1, -4]).at(-1)!.checks).toBe(7);
  });

  it('stops at the first positive captain', () => {
    expect(threeSumFrames([1, 2, 3, 4]).map((f) => f.kind)).toEqual(['sorted', 'stop-positive', 'done']);
  });

  it('counts brute-force triples as n choose 3', () => {
    expect(bruteForceChecks(6)).toBe(20);
    expect(bruteForceChecks(2)).toBe(0);
  });
});

describe('ThreeSumPlayground', () => {
  it('steps through decisions and shows the Java trace line', async () => {
    render(<ThreeSumPlayground />);
    expect(screen.getByText('sorted [-4, -1, -1, 0, 1, 2]')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('L=1(-1) R=5(2) sum=-3 -> too small, left++')).toBeInTheDocument();
  });

  it('rejects bad input and loads presets', async () => {
    render(<ThreeSumPlayground />);
    const input = screen.getByLabelText('Numbers');
    await userEvent.clear(input);
    await userEvent.type(input, '1, two, 3');
    await userEvent.click(screen.getByRole('button', { name: 'Use these' }));
    expect(screen.getByRole('alert')).toHaveTextContent('whole numbers');
    await userEvent.click(screen.getByRole('button', { name: 'All zeros' }));
    expect(screen.getByText('sorted [0, 0, 0, 0]')).toBeInTheDocument();
  });

  it('parses comma or space separated integers', () => {
    expect(parseNumbers('-1, 0 1,2')).toEqual([-1, 0, 1, 2]);
    expect(parseNumbers('1.5, 2')).toBeNull();
  });
});
