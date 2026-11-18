import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource-variable/fredoka';
import '@fontsource-variable/nunito';
import '@fontsource/jetbrains-mono/400.css';
import '@fontsource/jetbrains-mono/700.css';
import './app/theme.css';
import './shared/kit/kit.css';
import './shared/ui/ui.css';
import './app/app.css';
import { App } from './app/App';
import { chooseServices } from './app/services';
import { applyStoredTheme } from './app/theme';

applyStoredTheme();

chooseServices().then((services) => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App services={services} />
    </StrictMode>,
  );
});
