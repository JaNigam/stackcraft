import type { CharacterKind, Mood } from '@/shared/kit';

/** What Stackcraft teaches, told by the Kit's characters. Pure data so the home page stays a layout. */
export interface TourStep {
  kind: CharacterKind;
  mood: Mood;
  title: string;
  says: string;
}

export const learningLoop: TourStep[] = [
  {
    kind: 'blob',
    mood: 'happy',
    title: 'See it',
    says: 'Every idea starts as a cartoon, so you get the picture before the details.',
  },
  {
    kind: 'tree',
    mood: 'smug',
    title: 'Open it up',
    says: 'Then the exact internals on a chalkboard: real source, real numbers.',
  },
  {
    kind: 'queue',
    mood: 'surprised',
    title: 'Predict, then run',
    says: 'Guess the output, then run it on a real JVM. Every number here comes from a real run.',
  },
  {
    kind: 'map',
    mood: 'smug',
    title: 'Answer like the interview',
    says: 'Questions from warm-up to killer, with the answer an interviewer wants to hear.',
  },
  {
    kind: 'set',
    mood: 'nope',
    title: 'Keep your traps',
    says: 'Got one wrong? It goes on your Trap Board, and flashcards bring it back when it is due.',
  },
];

export const tracks: { label: string; tone: 'mint' | 'lavender' | 'pink' | 'amber' | 'sky'; ready: boolean }[] = [
  { label: 'Java Core', tone: 'mint', ready: true },
  { label: 'DSA Patterns', tone: 'sky', ready: true },
  { label: 'LLD', tone: 'lavender', ready: false },
  { label: 'HLD', tone: 'amber', ready: false },
  { label: 'Cloud', tone: 'pink', ready: false },
];
