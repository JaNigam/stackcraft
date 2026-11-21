import { Link } from 'react-router';
import { useSeriesList } from '@/entities/chapter/hooks';
import { WarmupDeck } from '@/features/flashcards/WarmupDeck';
import { Actor, Scene } from '@/shared/kit';
import { Badge, ErrorNote, Spinner } from '@/shared/ui';

const upcoming = ['LLD', 'HLD', 'Cloud'];

export function HomePage() {
  const series = useSeriesList();
  return (
    <div className="home">
      <section className="home-hero">
        <h1>
          Craft your stack, <span className="home-hero__hl">one internal at a time.</span>
        </h1>
        <p>Cartoon intuition first, exact internals second, then you prove it: predict the output, run the real JVM, answer like it is the interview.</p>
        <Scene label="Meet the cast">
          <Actor kind="list" label="ArrayList" says="I keep everything, in order." />
          <Actor kind="set" mood="nope" label="HashSet" says="Duplicates? Not on my list." />
          <Actor kind="map" mood="smug" label="HashMap" says="Give me a key, I give you a value." />
        </Scene>
      </section>

      <div className="home-grid">
        <section aria-labelledby="series-h">
          <h2 id="series-h">Series</h2>
          {series.isLoading && <Spinner />}
          {series.error && <ErrorNote error={series.error} />}
          <div className="home-series">
            {series.data?.map((s) => (
              <Link key={s.id} to={`/series/${s.id}`} className="ui-card home-series__card">
                <h3>{s.title}</h3>
                <p>{s.tagline}</p>
                <div className="qz-row">
                  <Badge tone="mint">{s.unitCount} unit{s.unitCount === 1 ? '' : 's'}</Badge>
                  <Badge tone="lavender">{s.chapterCount} chapter{s.chapterCount === 1 ? '' : 's'}</Badge>
                </div>
              </Link>
            ))}
            {upcoming.map((u) => (
              <div key={u} className="ui-card home-series__card is-soon" aria-disabled="true">
                <h3>{u}</h3>
                <p>Coming soon.</p>
              </div>
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
