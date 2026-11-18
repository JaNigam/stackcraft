package dev.stackcraft.api.content.adapter.web;

import dev.stackcraft.api.content.application.GetChapterUseCase;
import dev.stackcraft.api.content.application.ListSeriesUseCase;
import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.ChapterMeta;
import dev.stackcraft.api.content.domain.Series;
import dev.stackcraft.api.content.domain.Teaser;
import dev.stackcraft.api.content.port.ContentSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ContentController {

    private final ListSeriesUseCase listSeries;
    private final GetChapterUseCase getChapter;
    private final ContentSource content;

    public ContentController(ListSeriesUseCase listSeries, GetChapterUseCase getChapter, ContentSource content) {
        this.listSeries = listSeries;
        this.getChapter = getChapter;
        this.content = content;
    }

    @GetMapping("/series")
    public List<SeriesSummary> series() {
        return listSeries.all().stream().map(s -> new SeriesSummary(s.id(), s.title(), s.tagline(), s.units().size(),
                s.units().stream().mapToInt(u -> u.chapterIds().size()).sum())).toList();
    }

    @GetMapping("/series/{seriesId}")
    public SeriesView seriesById(@PathVariable String seriesId) {
        Series s = listSeries.one(seriesId);
        List<UnitView> units = s.units().stream().map(u -> new UnitView(u.id(), u.number(), u.title(), u.summary(),
                u.chapterIds().stream().map(id -> content.chapter(id).orElseThrow().meta()).map(ChapterSummary::of).toList()))
                .toList();
        return new SeriesView(s.id(), s.title(), s.tagline(), units);
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterView chapter(@PathVariable String chapterId) {
        Chapter c = getChapter.get(chapterId);
        return new ChapterView(c.meta(), c.teasers(), c.questions().stream().map(QuestionView::of).toList(),
                c.flashcards().size());
    }

    public record SeriesSummary(String id, String title, String tagline, int unitCount, int chapterCount) {}

    public record SeriesView(String id, String title, String tagline, List<UnitView> units) {}

    public record UnitView(String id, int number, String title, String summary, List<ChapterSummary> chapters) {}

    public record ChapterSummary(String id, int lesson, String title, String subtitle, int minutes) {
        static ChapterSummary of(ChapterMeta m) {
            return new ChapterSummary(m.id(), m.lesson(), m.title(), m.subtitle(), m.minutes());
        }
    }

    public record ChapterView(ChapterMeta meta, List<Teaser> teasers, List<QuestionView> questions, int flashcardCount) {}
}
