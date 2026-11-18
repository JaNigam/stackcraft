import { chapterFolder, contentRelative } from '@/entities/chapter/bundledContent';

/**
 * Chapter code and its verified output (.out), bundled at build time. The .out file is what the UI shows when the
 * runner is down, and it is checked against a real run by scripts/verify-content.mjs, so it is never stale.
 */
const raw = import.meta.glob<string>('@content/**/code/*.{java,out}', { query: '?raw', import: 'default', eager: true });
const byPath = new Map(Object.entries(raw).map(([k, v]) => [contentRelative(k), v]));

export interface CodeFile {
  name: string;
  source: string;
  verifiedOutput: string | undefined;
}

export function codeFile(chapterId: string, name: string): CodeFile | undefined {
  const folder = chapterFolder(chapterId);
  if (!folder) return undefined;
  const source = byPath.get(`${folder}/code/${name}`);
  if (source === undefined) return undefined;
  return { name, source, verifiedOutput: byPath.get(`${folder}/code/${name.replace(/\.java$/, '.out')}`) };
}
