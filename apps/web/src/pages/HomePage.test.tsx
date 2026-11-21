import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import { renderWithServices } from '@/test/renderWithServices';
import { HomePage } from './HomePage';
import { learningLoop } from './homeTour';

describe('HomePage', () => {
  it('explains the learning loop with one Kit character per step, in order', () => {
    renderWithServices(<HomePage />);
    const loop = screen.getByRole('region', { name: 'How every chapter works' });
    const steps = within(loop).getAllByRole('listitem');
    expect(steps).toHaveLength(learningLoop.length);
    learningLoop.forEach((s, i) => {
      expect(within(steps[i]).getByRole('heading', { name: s.title })).toBeInTheDocument();
      expect(within(steps[i]).getByRole('img', { name: `${s.kind}, looking ${s.mood}` })).toBeInTheDocument();
    });
  });

  it('links to both ready series and marks the rest as coming soon', () => {
    renderWithServices(<HomePage />);
    expect(screen.getByRole('link', { name: 'Start with Java Core' })).toHaveAttribute('href', '/series/java-core');
    expect(screen.getByRole('link', { name: 'Practise DSA patterns' })).toHaveAttribute('href', '/series/dsa');
    const tracks = screen.getByRole('list', { name: 'What you can learn' });
    expect(within(tracks).getByText('LLD · soon')).toBeInTheDocument();
  });
});
