import { api, unwrap } from '@/shared/api/http';
import type { AuthService, GuestImport } from './AuthService';

export class RemoteAuthService implements AuthService {
  async me() {
    const r = await api.GET('/api/v1/me');
    if (r.response.status === 401) return null;
    return unwrap(r);
  }

  async register(email: string, password: string, displayName: string) {
    return unwrap(await api.POST('/api/v1/auth/register', { body: { email, password, displayName } }));
  }

  async login(email: string, password: string) {
    return unwrap(await api.POST('/api/v1/auth/login', { body: { email, password } }));
  }

  async logout() {
    await api.POST('/api/v1/auth/logout');
  }

  async logoutEverywhere() {
    const r = await api.POST('/api/v1/auth/logout-all');
    if (!r.response.ok && r.response.status !== 401) throw new Error(`Logout failed (HTTP ${r.response.status})`);
  }

  async updateProfile(update: { displayName?: string; leaderboardVisible?: boolean }) {
    return unwrap(await api.PATCH('/api/v1/me', { body: update }));
  }

  async importGuestProgress(payload: GuestImport) {
    return unwrap(await api.POST('/api/v1/progress/imports', { body: payload }));
  }
}
