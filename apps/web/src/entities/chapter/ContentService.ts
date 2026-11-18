import type { ChapterView, SeriesSummary, SeriesView } from '@/shared/api/types';

/**
 * Port for reading content. Features depend on this, never on fetch or on the bundle (DIP).
 * Two adapters: RemoteContentService (the api) and BundledContentService (YAML compiled into the site).
 */
export interface ContentService {
  listSeries(): Promise<SeriesSummary[]>;
  getSeries(seriesId: string): Promise<SeriesView>;
  getChapter(chapterId: string): Promise<ChapterView>;
}
