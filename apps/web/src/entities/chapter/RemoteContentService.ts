import { api, unwrap } from '@/shared/api/http';
import type { ContentService } from './ContentService';

export class RemoteContentService implements ContentService {
  async listSeries() {
    return unwrap(await api.GET('/api/v1/series'));
  }

  async getSeries(seriesId: string) {
    return unwrap(await api.GET('/api/v1/series/{seriesId}', { params: { path: { seriesId } } }));
  }

  async getChapter(chapterId: string) {
    return unwrap(await api.GET('/api/v1/chapters/{chapterId}', { params: { path: { chapterId } } }));
  }
}
