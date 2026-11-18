import { useQuery } from '@tanstack/react-query';
import { useContent } from '@/app/services';

/** Container hooks: fetching lives here; components only draw (SRP). */
export function useSeriesList() {
  const content = useContent();
  return useQuery({ queryKey: ['series'], queryFn: () => content.listSeries() });
}

export function useSeries(seriesId: string) {
  const content = useContent();
  return useQuery({ queryKey: ['series', seriesId], queryFn: () => content.getSeries(seriesId) });
}

export function useChapter(chapterId: string) {
  const content = useContent();
  return useQuery({ queryKey: ['chapter', chapterId], queryFn: () => content.getChapter(chapterId) });
}
