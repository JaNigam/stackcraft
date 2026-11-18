import { Link, useParams } from 'react-router';
import { useSeries } from '@/entities/chapter/hooks';
import { useProgress } from '@/features/progress/hooks';
import { Badge, ErrorNote, Spinner } from '@/shared/ui';

export function SeriesPage() {
  const { seriesId = '' } = useParams();
  const series = useSeries(seriesId);
  const done = new Set(useProgress().data?.completedChapters ?? []);
  if (series.isLoading) return <Spinner />;
  if (series.error) return <ErrorNote error={series.error} />;
  const s = series.data!;
  return (
    <div className="series">
      <h1>{s.title}</h1>
      <p className="series__tagline">{s.tagline}</p>
      {s.units.map((u) => (
        <section key={u.id} className="series-unit">
          <h2>
            <span className="series-unit__num">Unit {u.number}</span> {u.title}
          </h2>
          {u.summary && <p>{u.summary}</p>}
          <ol className="series-chapters">
            {u.chapters.map((c) => (
              <li key={c.id}>
                <Link to={`/chapters/${c.id}`} className="ui-card series-chapter">
                  <span className="series-chapter__lesson">{u.number}.{c.lesson}</span>
                  <span className="series-chapter__text">
                    <strong>{c.title}</strong>
                    {c.subtitle && <small>{c.subtitle}</small>}
                  </span>
                  <span className="series-chapter__meta">
                    <Badge tone="sky">{c.minutes} min</Badge>
                    {done.has(c.id) && <Badge tone="ok">Done</Badge>}
                  </span>
                </Link>
              </li>
            ))}
          </ol>
        </section>
      ))}
    </div>
  );
}
