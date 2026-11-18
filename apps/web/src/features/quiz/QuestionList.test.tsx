import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { bundledContent } from '@/entities/chapter/bundledContent';
import { renderWithServices } from '@/test/renderWithServices';
import { questionRenderers } from './registry';
import { QuestionList } from './QuestionList';

const chapter = bundledContent().chapters.get('java-core.u00.l01')!.view;

describe('question renderer registry (OCP)', () => {
  it('has a renderer for every question type used in content', () => {
    for (const q of chapter.questions) expect(questionRenderers[q.type]).toBeTypeOf('function');
  });
});

describe('QuestionList', () => {
  it('groups by tier and grades an MCQ end to end', async () => {
    renderWithServices(<QuestionList chapterId={chapter.meta.id} questions={chapter.questions} />);
    expect(screen.getByRole('heading', { name: /Warm-up/ })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Killer/ })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /D\s+Map/ }));
    expect(await screen.findByText('Correct.')).toBeInTheDocument();
    expect(screen.getByText('+2 XP')).toBeInTheDocument();
  });

  it('grades a predict-the-output answer with the verified output', async () => {
    renderWithServices(<QuestionList chapterId={chapter.meta.id} questions={chapter.questions.filter((q) => q.id === 'i2')} />);
    await userEvent.type(screen.getByLabelText(/predicted output/), 'null{enter}2{enter}1 2 null');
    await userEvent.click(screen.getByRole('button', { name: /Lock in/ }));
    expect(await screen.findByText(/actually prints/)).toBeInTheDocument();
  });
});
