import { useSyncExternalStore } from 'react';
import { findTheme, systemTheme } from './themes/registry';
import { THEME_TOKENS, type ThemeDef } from './themes/types';

/**
 * Applies a theme by writing its tokens as CSS custom properties on <html>. The stylesheet only ever reads tokens,
 * so a new theme needs no CSS. The choice is either a theme id or "system" (follow the OS light/dark setting).
 */
export type ThemeChoice = string; // theme id or 'system'
const KEY = 'stackcraft.theme';
const darkQuery = () => matchMedia('(prefers-color-scheme: dark)');

let choice: ThemeChoice = 'system';
const listeners = new Set<() => void>();

export function storedChoice(): ThemeChoice {
  try {
    const v = localStorage.getItem(KEY);
    // old values from before themes existed
    if (v === 'light') return 'cream-paper';
    if (v === 'dark') return 'cocoa-night';
    return v && findTheme(v) ? v : 'system';
  } catch {
    return 'system';
  }
}

export function resolveTheme(c: ThemeChoice): ThemeDef {
  return findTheme(c) ?? systemTheme(darkQuery().matches ? 'dark' : 'light');
}

function paint() {
  const t = resolveTheme(choice);
  const root = document.documentElement;
  for (const token of THEME_TOKENS) root.style.setProperty(`--${token}`, t.tokens[token]);
  root.style.colorScheme = t.scheme;
  root.dataset.theme = t.id;
  root.dataset.scheme = t.scheme;
  listeners.forEach((l) => l());
}

export function setThemeChoice(c: ThemeChoice) {
  choice = c;
  try {
    if (c === 'system') localStorage.removeItem(KEY);
    else localStorage.setItem(KEY, c);
  } catch {
    /* blocked storage: theme for this visit only */
  }
  paint();
}

let watching = false;

export function applyStoredTheme() {
  choice = storedChoice();
  paint();
  if (!watching) {
    watching = true;
    darkQuery().addEventListener?.('change', () => choice === 'system' && paint());
  }
}

export function isDark(): boolean {
  return document.documentElement.dataset.scheme === 'dark';
}

/** React view of the current choice and resolved theme. */
export function useTheme(): { choice: ThemeChoice; theme: ThemeDef } {
  const snapshot = useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => `${choice}|${document.documentElement.dataset.theme ?? ''}`,
  );
  const [c] = snapshot.split('|');
  return { choice: c, theme: resolveTheme(c) };
}
