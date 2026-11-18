import { describe, expect, it } from 'vitest';
import { themes, systemTheme } from './registry';
import { THEME_TOKENS } from './types';

/** Guards for plug-and-play themes: a new theme file must pass these without any other change. */
function luminance(hex: string): number {
  const n = hex.replace('#', '');
  const [r, g, b] = [0, 2, 4].map((i) => {
    const c = parseInt(n.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string): number {
  const [l1, l2] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}

const isHex = (v: string) => /^#[0-9a-f]{6}$/i.test(v);

describe('theme registry', () => {
  it('finds the five built-in themes with unique ids', () => {
    expect(themes.map((t) => t.id)).toEqual(['cream-paper', 'cocoa-night', 'midnight-ink', 'forest-chalk', 'plum-dusk']);
    expect(new Set(themes.map((t) => t.id)).size).toBe(themes.length);
  });

  it('has a System default for both light and dark', () => {
    expect(systemTheme('light').id).toBe('cream-paper');
    expect(systemTheme('dark').id).toBe('cocoa-night');
    for (const scheme of ['light', 'dark'] as const) {
      expect(themes.filter((t) => t.scheme === scheme && t.systemDefault).length).toBeLessThanOrEqual(1);
    }
  });
});

describe.each(themes.map((t) => [t.id, t] as const))('theme %s', (_id, t) => {
  it('defines every token with a non-empty value', () => {
    for (const token of THEME_TOKENS) expect(t.tokens[token], token).toBeTruthy();
    expect(Object.keys(t.tokens).sort()).toEqual([...THEME_TOKENS].sort());
  });

  it('keeps text readable (WCAG AA or better)', () => {
    expect(contrast(t.tokens.text, t.tokens.bg)).toBeGreaterThanOrEqual(7);
    expect(contrast(t.tokens.text, t.tokens.surface)).toBeGreaterThanOrEqual(7);
    expect(contrast(t.tokens['text-muted'], t.tokens.surface)).toBeGreaterThanOrEqual(4.5);
    expect(contrast(t.tokens['code-text'], t.tokens['code-bg'])).toBeGreaterThanOrEqual(7);
  });

  it('uses plain hex for the colours the contrast check reads', () => {
    for (const k of ['bg', 'surface', 'text', 'text-muted', 'code-bg', 'code-text'] as const) expect(isHex(t.tokens[k]), k).toBe(true);
  });

  if (t.scheme === 'dark') {
    it('avoids the high-contrast look: outlines stay soft, not near-white', () => {
      expect(luminance(t.tokens.outline)).toBeLessThan(0.2);
    });
  }
});
