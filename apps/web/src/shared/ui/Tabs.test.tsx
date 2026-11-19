import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Tabs } from './index';

const tabs = [
  { id: 'a', label: 'A', content: <h2 id="in-a">Inside A</h2> },
  { id: 'b', label: 'B', content: <p>Inside B</p> },
];

describe('Tabs', () => {
  it('by default renders only the active panel', async () => {
    render(<Tabs tabs={tabs} />);
    expect(screen.getByText('Inside A')).toBeInTheDocument();
    expect(screen.queryByText('Inside B')).toBeNull();
    await userEvent.click(screen.getByRole('tab', { name: 'B' }));
    expect(screen.queryByText('Inside A')).toBeNull();
  });

  it('keepMounted keeps hidden panels in the DOM so their headings can be found', async () => {
    render(<Tabs tabs={tabs} initial="b" keepMounted />);
    expect(document.getElementById('in-a')).not.toBeNull();
    expect(screen.getByText('Inside A').closest('[role=tabpanel]')).toHaveAttribute('hidden');
    await userEvent.click(screen.getByRole('tab', { name: 'A' }));
    expect(screen.getByText('Inside A').closest('[role=tabpanel]')).not.toHaveAttribute('hidden');
  });
});
