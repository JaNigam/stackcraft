import { ApiError } from '@/shared/api/http';
import type { ContentService } from './ContentService';
import { bundledContent } from './bundledContent';

/** Offline adapter: serves the content compiled into the site. Same shapes as the api. */
export class BundledContentService implements ContentService {
  async listSeries() {
    return bundledContent().series.map((s) => ({
      id: s.id,
      title: s.title,
      tagline: s.tagline,
      unitCount: s.units.length,
      chapterCount: s.units.reduce((n, u) => n + u.chapters.length, 0),
    }));
  }

  async getSeries(seriesId: string) {
    const s = bundledContent().series.find((x) => x.id === seriesId);
    if (!s) throw new ApiError(404, 'not-found', `Series '${seriesId}' not found`);
    return s;
  }

  async getChapter(chapterId: string) {
    const c = bundledContent().chapters.get(chapterId);
    if (!c) throw new ApiError(404, 'not-found', `Chapter '${chapterId}' not found`);
    return c.view;
  }
}
