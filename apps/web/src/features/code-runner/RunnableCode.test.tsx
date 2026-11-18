import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithServices } from '@/test/renderWithServices';
import { ChapterContextProvider } from '@/features/theory/ChapterContext';
import { RunnableCode } from './RunnableCode';
import type { CodeRunner } from './CodeRunner';

const ui = (
  <ChapterContextProvider chapterId="java-core.u00.l01">
    <RunnableCode file="CollectionsTour.java" />
  </ChapterContextProvider>
);

describe('RunnableCode', () => {
  it('falls back to the verified output when the runner is unavailable', async () => {
    renderWithServices(ui); // offline services: runner always unavailable
    expect(screen.getByRole('region', { name: 'Code: CollectionsTour.java' }).textContent).toContain('public class CollectionsTour');
    await userEvent.click(screen.getByRole('button', { name: /Run/ }));
    expect(await screen.findByText('Stored output')).toBeInTheDocument();
    expect(screen.getByText(/bucket=1 of 16/)).toBeInTheDocument();
  });

  it('shows the live result when the runner answers', async () => {
    const runner: CodeRunner = {
      run: async () => ({ kind: 'ran', result: { status: 'OK', stdout: 'live output\n', stderr: '', exitCode: 0, durationMs: 321, truncated: false } }),
    };
    renderWithServices(ui, { runner });
    await userEvent.click(screen.getByRole('button', { name: /Run/ }));
    expect(await screen.findByText('live output')).toBeInTheDocument();
    expect(screen.getByText(/in 321 ms/)).toBeInTheDocument();
  });
});
