import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { Character } from '@/shared/kit';
import { Button } from '@/shared/ui';
import { ApiError } from '@/shared/api/http';
import { useSessionApi } from './session';

type Mode = 'signin' | 'signup';

/** Mirrors the server rules so most mistakes are caught before a round trip; the server stays the authority. */
export function validate(mode: Mode, email: string, password: string, name: string): string | null {
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/.test(email.trim())) return 'Enter a valid email address';
  if (mode === 'signup') {
    if (password.length < 10) return 'Use at least 10 characters for your password';
    if (password.trim().toLowerCase() === email.trim().toLowerCase()) return 'Your password must not be your email';
    const n = name.trim();
    if (n.length < 2 || n.length > 32) return 'Display name: 2 to 32 characters';
  } else if (!password) {
    return 'Enter your password';
  }
  return null;
}

function strength(pw: string): { label: string; level: 0 | 1 | 2 | 3 } {
  if (pw.length < 10) return { label: 'Too short', level: 0 };
  const kinds = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((r) => r.test(pw)).length;
  if (pw.length >= 16 || (pw.length >= 12 && kinds >= 3)) return { label: 'Strong', level: 3 };
  if (pw.length >= 12 || kinds >= 3) return { label: 'Good', level: 2 };
  return { label: 'OK', level: 1 };
}

export function AuthPage({ mode }: { mode: Mode }) {
  const session = useSessionApi();
  const navigate = useNavigate();
  const location = useLocation();
  const back = (location.state as { from?: string } | null)?.from ?? '/';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [show, setShow] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!session.available) {
    return (
      <div className="auth">
        <div className="ui-card auth__card">
          <h1>Accounts need the api</h1>
          <p>This copy of Stackcraft runs without a backend, so progress is saved in this browser only.</p>
          <Link to="/">Back home</Link>
        </div>
      </div>
    );
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const problem = validate(mode, email, password, name);
    setError(problem);
    if (problem) return;
    setBusy(true);
    try {
      if (mode === 'signup') await session.signUp(email.trim(), password, name.trim());
      else await session.signIn(email.trim(), password);
      navigate(back, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Try again.');
    } finally {
      setBusy(false);
    }
  };

  const pw = strength(password);
  return (
    <div className="auth">
      <form className="ui-card auth__card" onSubmit={submit} noValidate aria-labelledby="auth-title">
        <div className="auth__mascot" aria-hidden="true">
          <Character kind="map" mood={error ? 'surprised' : 'happy'} size={72} />
        </div>
        <h1 id="auth-title">{mode === 'signup' ? 'Create your account' : 'Welcome back'}</h1>
        <p className="auth__lead">
          {mode === 'signup' ? 'Your XP, streak and Trap Board follow you to every device.' : 'Sign in to pick up where you left off.'}
        </p>

        <label className="auth__field">
          <span>Email</span>
          <input className="ui-input" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>

        {mode === 'signup' && (
          <label className="auth__field">
            <span>Display name</span>
            <input className="ui-input" autoComplete="nickname" maxLength={32} value={name} onChange={(e) => setName(e.target.value)} required />
            <small>Shown on the leaderboard. Never your email.</small>
          </label>
        )}

        <label className="auth__field">
          <span>Password</span>
          <span className="auth__pw">
            <input
              className="ui-input"
              type={show ? 'text' : 'password'}
              autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <Button variant="ghost" onClick={() => setShow((s) => !s)} aria-pressed={show}>
              {show ? 'Hide' : 'Show'}
            </Button>
          </span>
          {mode === 'signup' && (
            <span className={`auth__meter auth__meter--${pw.level}`} aria-live="polite">
              <span />
              <span />
              <span />
              <em>{pw.label}</em>
            </span>
          )}
        </label>

        {error && (
          <p className="auth__error" role="alert">
            {error}
          </p>
        )}

        <Button type="submit" variant="primary" disabled={busy}>
          {busy ? 'One moment…' : mode === 'signup' ? 'Create account' : 'Sign in'}
        </Button>

        <p className="auth__note">Progress you earned in this browser as a guest is added to your account when you sign in.</p>
        <p className="auth__switch">
          {mode === 'signup' ? (
            <>
              Already have an account?{' '}
              <Link to="/signin" state={{ from: back }}>
                Sign in
              </Link>
            </>
          ) : (
            <>
              New here?{' '}
              <Link to="/signup" state={{ from: back }}>
                Create an account
              </Link>
            </>
          )}
        </p>
      </form>
    </div>
  );
}
