import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router';
import { useSessionApi } from './session';

/** Header entry: "Sign in" for guests, a menu with profile settings and logout for a signed-in learner. */
export function AccountMenu() {
  const session = useSessionApi();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
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

  if (!session.available || session.state.status === 'loading') return null;
  if (session.state.status === 'guest') {
    return (
      <Link to="/signin" state={{ from: location.pathname }} className="ui-btn ui-btn--primary acct-signin">
        Sign in
      </Link>
    );
  }

  const me = session.state.me;
  const run = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await fn();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="tm" ref={ref}>
      <button type="button" className="ui-btn ui-btn--ghost acct-trigger" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <span className="acct-avatar" aria-hidden="true">
          {me.displayName.slice(0, 1).toUpperCase()}
        </span>
        <span className="acct-name">{me.displayName}</span>
      </button>
      {open && (
        <div className="tm-menu" role="menu" aria-label="Account">
          <div className="acct-who">
            <strong>{me.displayName}</strong>
            <small>{me.email}</small>
          </div>
          <label className="tm-item acct-toggle" role="menuitemcheckbox" aria-checked={me.leaderboardVisible}>
            <input
              type="checkbox"
              checked={me.leaderboardVisible}
              disabled={busy}
              onChange={(e) => run(() => session.updateProfile({ leaderboardVisible: e.target.checked }))}
            />
            <span>
              <strong>Show me on the leaderboard</strong>
              <small>Only your display name and XP are shown.</small>
            </span>
          </label>
          <button type="button" role="menuitem" className="tm-item" disabled={busy} onClick={() => run(async () => { await session.signOut(); setOpen(false); })}>
            <span>
              <strong>Log out</strong>
              <small>On this device. Your progress stays in your account.</small>
            </span>
          </button>
          <button type="button" role="menuitem" className="tm-item" disabled={busy} onClick={() => run(async () => { await session.signOutEverywhere(); setOpen(false); })}>
            <span>
              <strong>Log out everywhere</strong>
              <small>Ends every session on every device.</small>
            </span>
          </button>
        </div>
      )}
    </div>
  );
}
