import { Link } from 'react-router';
import { useSeriesList } from '@/entities/chapter/hooks';
import { WarmupDeck } from '@/features/flashcards/WarmupDeck';
import { Character } from '@/shared/kit';
import { Badge, ErrorNote } from '@/shared/ui';
import { curriculum, method, type Track } from './homeTour';

export function HomePage() {
  const series = useSeriesList();
  const chapters = new Map(series.data?.map((s) => [s.id, s.chapterCount]));

  return (
    <div className="home">
      <section className="home-hero" aria-labelledby="hero-h">
        <p className="home-hero__eyebrow">Interview preparation for Java engineers</p>
        <h1 id="hero-h">Understand the internals. Practise the interview.</h1>
        <p className="home-hero__lead">
          Stackcraft teaches Java, data structures and algorithms, and system design the way senior interviews test
          them: how things work underneath, why they were built that way, and how to explain it clearly.
        </p>
        <div className="home-hero__cta">
          <Link to="/series/java-core" className="ui-btn ui-btn--primary">
            Start with Java Core
          </Link>
          <Link to="/series/dsa" className="ui-btn ui-btn--default">
            Browse DSA patterns
          </Link>
        </div>
        <ol className="home-method" aria-label="How each chapter is built">
          {method.map((m, i) => (
            <li key={m.title}>
              <span className="home-method__num" aria-hidden="true">
                {String(i + 1).padStart(2, '0')}
              </span>
              <strong>{m.title}</strong>
              <span>{m.body}</span>
            </li>
          ))}
        </ol>
      </section>

      <div className="home-grid">
        <section aria-labelledby="curriculum-h">
          <h2 id="curriculum-h">What you will learn</h2>
          {series.error && <ErrorNote error={series.error} />}
          <div className="home-tracks">
            {curriculum.map((t) => (
              <TrackCard key={t.title} track={t} chapterCount={t.seriesId ? chapters.get(t.seriesId) : undefined} />
            ))}
          </div>
        </section>
        <aside aria-label="Today">
          <h2>Today</h2>
          <WarmupDeck />
        </aside>
      </div>
    </div>
  );
}

function TrackCard({ track, chapterCount }: { track: Track; chapterCount?: number }) {
  const body = (
    <>
      <div className="home-track__head">
        <Character kind={track.kind} size={48} />
        <div>
          <h3>{track.title}</h3>
          {track.seriesId ? (
            <Badge tone="mint">{chapterCount === undefined ? 'Available' : `${chapterCount} chapter${chapterCount === 1 ? '' : 's'}`}</Badge>
          ) : (
            <Badge tone="lavender">Coming soon</Badge>
          )}
        </div>
      </div>
      <p>{track.summary}</p>
      <ul>
        {track.topics.map((topic) => (
          <li key={topic}>{topic}</li>
        ))}
      </ul>
    </>
  );
  return track.seriesId ? (
    <Link to={`/series/${track.seriesId}`} className="ui-card home-track" aria-label={`${track.title} series`}>
      {body}
    </Link>
  ) : (
    <div className="ui-card home-track is-soon">{body}</div>
  );
}
