import { useState } from 'react';
import { BrowserRouter, Link, NavLink, Route, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ServicesProvider, type Services } from './services';
import { ThemeMenu } from './ThemeMenu';
import { ProgressHeader } from '@/features/progress/ProgressHeader';
import { HomePage } from '@/pages/HomePage';
import { SeriesPage } from '@/pages/SeriesPage';
import { ChapterPage } from '@/pages/ChapterPage';
import { TrapBoardPage } from '@/pages/TrapBoardPage';
import { KitPage } from '@/pages/KitPage';
import { Character } from '@/shared/kit';
import { SessionProvider } from '@/features/auth/session';
import { AuthPage } from '@/features/auth/AuthPage';
import { AccountMenu } from '@/features/auth/AccountMenu';

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
            <NavLink to="/series/dsa">DSA</NavLink>
            <NavLink to="/traps">Trap Board</NavLink>
            <NavLink to="/kit">Kit</NavLink>
          </nav>
          <ThemeMenu />
          <AccountMenu />
        </div>
        <div className="app-header__row app-header__row--progress">
          <ProgressHeader />
          <span className={`app-mode app-mode--${mode}`} title={mode === 'online' ? 'Connected to the api' : 'No api: content from the bundle, progress saved in this browser'}>
            {mode === 'online' ? 'Online' : import.meta.env.VITE_STATIC_SITE === 'true' ? 'Static preview' : 'Offline mode'}
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
          <Route path="/signin" element={<AuthPage mode="signin" />} />
          <Route path="/signup" element={<AuthPage mode="signup" />} />
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
        <SessionProvider auth={services.auth} guestProgress={services.guestProgress}>
          <BrowserRouter basename={import.meta.env.BASE_URL.replace(/\/$/, '') || undefined}>
            <Shell mode={services.mode} />
          </BrowserRouter>
        </SessionProvider>
      </ServicesProvider>
    </QueryClientProvider>
  );
}
