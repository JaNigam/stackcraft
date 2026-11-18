import { useEffect, useRef, useState } from 'react';
import { themes } from './themes/registry';
import { setThemeChoice, useTheme } from './theme';

function Swatch({ colors }: { colors: readonly string[] }) {
  return (
    <span className="tm-swatch" aria-hidden="true">
      {colors.map((c, i) => (
        <span key={i} style={{ background: c }} />
      ))}
    </span>
  );
}

/** Header theme picker. Lists every theme the registry found, plus "System". */
export function ThemeMenu() {
  const { choice, theme } = useTheme();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', esc);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', esc);
    };
  }, [open]);

  const pick = (id: string) => {
    setThemeChoice(id);
    setOpen(false);
  };

  return (
    <div className="tm" ref={ref}>
      <button type="button" className="ui-btn ui-btn--ghost tm-trigger" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <Swatch colors={theme.swatch} />
        <span className="tm-trigger__label">{choice === 'system' ? 'System' : theme.label}</span>
      </button>
      {open && (
        <div className="tm-menu" role="menu" aria-label="Theme">
          <button type="button" role="menuitemradio" aria-checked={choice === 'system'} className="tm-item" onClick={() => pick('system')}>
            <Swatch colors={['#f7f1e3', '#fff8ec', '#1e1a17', '#2a2420']} />
            <span>
              <strong>System</strong>
              <small>Follow your device: light or dark</small>
            </span>
          </button>
          {(['light', 'dark'] as const).map((scheme) => (
            <div key={scheme} role="group" aria-label={scheme === 'light' ? 'Light themes' : 'Dark themes'}>
              <div className="tm-group">{scheme === 'light' ? 'Light' : 'Dark'}</div>
              {themes
                .filter((t) => t.scheme === scheme)
                .map((t) => (
                  <button key={t.id} type="button" role="menuitemradio" aria-checked={choice === t.id} className="tm-item" onClick={() => pick(t.id)}>
                    <Swatch colors={t.swatch} />
                    <span>
                      <strong>{t.label}</strong>
                      <small>{t.description}</small>
                    </span>
                  </button>
                ))}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
