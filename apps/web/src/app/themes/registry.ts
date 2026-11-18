import type { ThemeDef } from './types';

/**
 * Plug-and-play: every `*.theme.ts` file in this folder is a theme. Nothing else needs editing to add one.
 * (Registry pattern again, same idea as the question renderers and the api's GraderRegistry.)
 */
const modules = import.meta.glob<{ default: ThemeDef }>('./*.theme.ts', { eager: true });

export const themes: readonly ThemeDef[] = Object.values(modules)
  .map((m) => m.default)
  .sort((a, b) => a.order - b.order || a.label.localeCompare(b.label));

export function findTheme(id: string | null | undefined): ThemeDef | undefined {
  return themes.find((t) => t.id === id);
}

/** The theme "System" resolves to for a given OS scheme. */
export function systemTheme(scheme: 'light' | 'dark'): ThemeDef {
  return themes.find((t) => t.scheme === scheme && t.systemDefault) ?? themes.find((t) => t.scheme === scheme) ?? themes[0];
}
