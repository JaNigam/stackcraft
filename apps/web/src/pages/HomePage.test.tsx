import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import { renderWithServices } from '@/test/renderWithServices';
import { HomePage } from './HomePage';
import { curriculum, method } from './homeTour';

describe('HomePage', () => {
  it('states what Stackcraft teaches and how each chapter is built', () => {
    renderWithServices(<HomePage />);
    expect(screen.getByRole('heading', { level: 1, name: 'Understand the internals. Practise the interview.' })).toBeInTheDocument();
    const steps = within(screen.getByRole('list', { name: 'How each chapter is built' })).getAllByRole('listitem');
    expect(steps.map((s) => s.querySelector('strong')?.textContent)).toEqual(method.map((m) => m.title));
  });

  it('links every available track to its series and marks the rest as coming soon', () => {
    renderWithServices(<HomePage />);
    const section = screen.getByRole('region', { name: 'What you will learn' });
    for (const t of curriculum) {
      expect(within(section).getByRole('heading', { name: t.title })).toBeInTheDocument();
      if (t.seriesId) {
        expect(within(section).getByRole('link', { name: `${t.title} series` })).toHaveAttribute('href', `/series/${t.seriesId}`);
      }
    }
    expect(within(section).getAllByText('Coming soon')).toHaveLength(curriculum.filter((t) => !t.seriesId).length);
    expect(screen.getByRole('link', { name: 'Start with Java Core' })).toHaveAttribute('href', '/series/java-core');
  });
});
