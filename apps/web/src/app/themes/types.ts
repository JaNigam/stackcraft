/**
 * A theme is pure data: a name, whether it is light or dark, and a value for every token below.
 * To add a theme, drop a `<id>.theme.ts` file in this folder exporting `defineTheme({...})`. The registry finds it,
 * the header menu lists it, and `themes.test.ts` checks it defines every token with readable contrast.
 */
export const THEME_TOKENS = [
  // page and surfaces
  'bg', // page background
  'bg-dots', // dot-grid pattern on the page
  'header-bg', // sticky header (slightly translucent page colour)
  'surface', // cards, panels
  'surface-2', // nested or hovered surfaces, hints
  'callout-bg', // body of Callout boxes
  // text and lines
  'text',
  'text-muted',
  'outline', // card and control borders (keep it soft in dark themes: not pure white)
  'sticker-edge', // die-cut edge around stickers so ink outlines read on dark pages
  'shadow', // hard offset shadow for cards
  'shadow-sm',
  'focus',
  'highlight', // marker behind highlighted words
  'arrow-filter', // CSS filter for the hand-drawn ink arrows (none on light, invert(...) on dark)
  // feedback
  'ok',
  'ok-bg',
  'bad',
  'bad-bg',
  // code
  'code-bg',
  'code-text',
  'tk-keyword',
  'tk-type',
  'tk-string',
  'tk-number',
  'tk-comment',
  'tk-annotation',
] as const;

export type ThemeToken = (typeof THEME_TOKENS)[number];

export interface ThemeDef {
  /** Stable id, stored in the browser and set as <html data-theme>. kebab-case. */
  id: string;
  label: string;
  /** Short line for the menu. */
  description: string;
  scheme: 'light' | 'dark';
  /** Menu position (lower first). */
  order: number;
  /** Used when the learner picks "System" and the OS is in this scheme. One per scheme. */
  systemDefault?: boolean;
  /** Four colours for the menu swatch: page, surface, outline, text. */
  swatch: [string, string, string, string];
  tokens: Record<ThemeToken, string>;
}

/** Identity helper: gives theme files type checking and autocomplete on token names. */
export function defineTheme(t: ThemeDef): ThemeDef {
  return t;
}

/** Code colours shared by the dark themes (tuned for any dark surface). */
export const darkCodeTokens = {
  'tk-keyword': '#c8a2ff',
  'tk-type': '#7fd1f5',
  'tk-string': '#ffa28f',
  'tk-number': '#fac775',
  'tk-comment': '#a39b8c',
  'tk-annotation': '#9fe1cb',
} satisfies Partial<Record<ThemeToken, string>>;
