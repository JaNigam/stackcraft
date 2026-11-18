import { useState } from 'react';
import { BrowserRouter, Link, NavLink, Route, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ServicesProvider, type Services } from './services';
import { isDark, setTheme } from './theme';
import { ProgressHeader } from '@/features/progress/ProgressHeader';
import { HomePage } from '@/pages/HomePage';
import { SeriesPage } from '@/pages/SeriesPage';
import { ChapterPage } from '@/pages/ChapterPage';
import { TrapBoardPage } from '@/pages/TrapBoardPage';
import { KitPage } from '@/pages/KitPage';
import { Character } from '@/shared/kit';

function ThemeToggle() {
  const [dark, setDark] = useState(isDark());
  return (
    <button
      type="button"
      className="ui-btn ui-btn--ghost app-theme"
      aria-label={dark ? 'Switch to light theme' : 'Switch to dark theme'}
      onClick={() => {
        setTheme(dark ? 'light' : 'dark');
        setDark(!dark);
      }}
    >
      {dark ? '☀' : '☾'}
    </button>
  );
}

function Shell({ mode }: { mode: Services['mode'] }) {
  return (
    <div className="app">
      <a href="#main" className="app-skip">
        Skip to content
      </a>
      <header className="app-header">
        <div className="app-header__row">
          <Link to="/" className="app-logo" aria-label="Stackcraft home">
            <Character kind="list" mood="happy" size={34} />
            <span>Stackcraft</span>
          </Link>
          <nav className="app-nav" aria-label="Main">
            <NavLink to="/series/java-core">Java Core</NavLink>
            <NavLink to="/traps">Trap Board</NavLink>
            <NavLink to="/kit">Kit</NavLink>
          </nav>
          <ThemeToggle />
        </div>
        <div className="app-header__row app-header__row--progress">
          <ProgressHeader />
          <span className={`app-mode app-mode--${mode}`} title={mode === 'online' ? 'Connected to the api' : 'No api: content from the bundle, progress saved in this browser'}>
            {mode === 'online' ? 'Online' : 'Offline mode'}
          </span>
        </div>
      </header>
      <main id="main" className="app-main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/series/:seriesId" element={<SeriesPage />} />
          <Route path="/chapters/:chapterId" element={<ChapterPage />} />
          <Route path="/traps" element={<TrapBoardPage />} />
          <Route path="/kit" element={<KitPage />} />
          <Route
            path="*"
            element={
              <div className="app-404">
                <Character kind="blob" mood="sad" label="Nothing here" />
                <Link to="/">Back home</Link>
              </div>
            }
          />
        </Routes>
      </main>
      <footer className="app-footer">Stackcraft: craft your stack, one internal at a time.</footer>
    </div>
  );
}

export function App({ services }: { services: Services }) {
  const [client] = useState(() => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false } } }));
  return (
    <QueryClientProvider client={client}>
      <ServicesProvider value={services}>
        <BrowserRouter>
          <Shell mode={services.mode} />
        </BrowserRouter>
      </ServicesProvider>
    </QueryClientProvider>
  );
}
