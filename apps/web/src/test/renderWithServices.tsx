import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { ServicesProvider, offlineServices, type Services } from '@/app/services';

/** Renders with real offline adapters by default; pass overrides to fake one port. */
export function renderWithServices(ui: ReactElement, overrides: Partial<Services> = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const services = { ...offlineServices(), ...overrides };
  return render(
    <QueryClientProvider client={client}>
      <ServicesProvider value={services}>
        <MemoryRouter>{ui}</MemoryRouter>
      </ServicesProvider>
    </QueryClientProvider>,
  );
}
