import type { ComponentType } from 'react';
import type { QuestionType } from '@/shared/api/types';
import type { QuestionRendererProps } from './types';
import { McqCard } from './renderers/McqCard';
import { PredictCard } from './renderers/PredictCard';
import { ShortAnswerCard } from './renderers/ShortAnswerCard';
import { ProblemCard } from './renderers/ProblemCard';

/**
 * Renderer registry (OCP). A new question type adds one entry here; QuestionList never changes. Typed as a Record
 * over the generated QuestionType union, so adding a type to openapi.yaml without a renderer fails to compile.
 */
export const questionRenderers: Record<QuestionType, ComponentType<QuestionRendererProps>> = {
  mcq: McqCard,
  predict: PredictCard,
  short: ShortAnswerCard,
  problem: ProblemCard,
};

export const typeLabel: Record<QuestionType, string> = {
  mcq: 'Multiple choice',
  predict: 'Predict the output',
  short: 'Short answer',
  problem: 'Killer problem',
};
