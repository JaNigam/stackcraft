import type { CharacterKind } from '@/shared/kit';

/** Home page copy as data, so the page stays a layout and the curriculum is easy to update. */
export interface Track {
  title: string;
  /** Series id when it has chapters; undefined while it is still being written. */
  seriesId?: string;
  kind: CharacterKind;
  summary: string;
  topics: string[];
}

export const curriculum: Track[] = [
  {
    title: 'Java Core',
    seriesId: 'java-core',
    kind: 'list',
    summary: 'How the language and the JVM behave under the hood.',
    topics: ['Collections and hashing', 'Memory: stack, heap, GC', 'Strings, OOP and generics', 'Streams and concurrency'],
  },
  {
    title: 'DSA Patterns',
    seriesId: 'dsa',
    kind: 'queue',
    summary: 'Interview problems grouped by the idea that solves them.',
    topics: ['Two pointers', 'Sliding window', 'Binary search', 'Trees, graphs and DP'],
  },
  {
    title: 'Low-level design',
    kind: 'tree',
    summary: 'Clean object design, taught on this codebase.',
    topics: ['SOLID in practice', 'Design patterns', 'Ports and adapters', 'Machine-coding rounds'],
  },
  {
    title: 'High-level design',
    kind: 'map',
    summary: 'System design, starting from how Stackcraft is built.',
    topics: ['Service boundaries and APIs', 'Caching and queues', 'Data stores and scaling', 'Security and sandboxing'],
  },
  {
    title: 'Cloud',
    kind: 'set',
    summary: 'Ship it: the series ends with Stackcraft deployed.',
    topics: ['Containers', 'CI/CD pipelines', 'Kubernetes and Terraform', 'Observability'],
  },
];

export const method: { title: string; body: string }[] = [
  { title: 'Concept', body: 'A short explanation with diagrams, before any detail.' },
  { title: 'Internals', body: 'The real source and the real numbers behind it.' },
  { title: 'Run the code', body: 'Predict the output, then run it on a real JVM.' },
  { title: 'Interview practice', body: 'Graded questions, model answers and spaced review.' },
];
