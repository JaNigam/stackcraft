import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CollectionsPlayground, simulate } from './CollectionsPlayground';

describe('CollectionsPlayground', () => {
  it('replays the tour exactly as CollectionsTour.out prints it', () => {
    const s = simulate(['mango', 'kiwi', 'apple', 'banana', 'apple']);
    expect(`[${s.list.join(', ')}]`).toBe('[mango, kiwi, apple, banana, apple]');
    expect(`[${s.hash.values().join(', ')}]`).toBe('[banana, apple, kiwi, mango]');
    expect(`[${s.linked.join(', ')}]`).toBe('[mango, kiwi, apple, banana]');
    expect(`[${s.tree.join(', ')}]`).toBe('[apple, banana, kiwi, mango]');
    expect(s.results).toEqual([true, true, true, true, false]);
  });

  it('shows a rejected duplicate and the bucket maths', async () => {
    render(<CollectionsPlayground />);
    await userEvent.click(screen.getByRole('button', { name: 'apple' }));
    await userEvent.click(screen.getByRole('button', { name: 'apple' }));
    expect(screen.getByText(/already in bucket 1: add\(\) returns false/)).toBeInTheDocument();
    expect(screen.getByText(/hashCode\(\) = 93029210/)).toBeInTheDocument();
  });
});
