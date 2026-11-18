package dev.stackcraft.api.content.adapter.fs;

import dev.stackcraft.api.content.adapter.fs.YamlModels.ChapterFile;
import dev.stackcraft.api.content.adapter.fs.YamlModels.FlashcardFile;
import dev.stackcraft.api.content.adapter.fs.YamlModels.QuestionsFile;
import dev.stackcraft.api.content.adapter.fs.YamlModels.SeriesFile;
import dev.stackcraft.api.content.adapter.fs.YamlModels.TeaserFile;
import dev.stackcraft.api.content.adapter.fs.YamlModels.TeasersFile;
import dev.stackcraft.api.content.adapter.fs.YamlModels.UnitFile;
import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.ChapterMeta;
import dev.stackcraft.api.content.domain.Flashcard;
import dev.stackcraft.api.content.domain.Series;
import dev.stackcraft.api.content.domain.Teaser;
import dev.stackcraft.api.quiz.domain.Question;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads {@code content/<series>/series.yaml} and every chapter folder it lists. Fails loudly with the file name on
 * any problem: bad content should break the build ({@code verify-content}) or start-up, never a learner's page.
 */
final class ContentLoader {

    private final ObjectMapper yaml = YAMLMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    ContentSnapshot load(Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Content root not found: " + root.toAbsolutePath().normalize());
        }
        List<Series> series = new ArrayList<>();
        Map<String, Chapter> chapters = new HashMap<>();
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path seriesDir : dirs.filter(d -> Files.isRegularFile(d.resolve("series.yaml"))).sorted().toList()) {
                series.add(loadSeries(seriesDir, chapters));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ContentSnapshot(series, chapters);
    }

    private Series loadSeries(Path seriesDir, Map<String, Chapter> chapters) {
        SeriesFile sf = read(seriesDir.resolve("series.yaml"), SeriesFile.class);
        List<Series.Unit> units = new ArrayList<>();
        for (UnitFile uf : nonNull(sf.units())) {
            List<String> chapterIds = new ArrayList<>();
            for (String folder : nonNull(uf.chapters())) {
                Path chapterDir = seriesDir.resolve(uf.dir()).resolve(folder);
                Chapter chapter = loadChapter(chapterDir, sf.id(), uf.id());
                if (chapters.putIfAbsent(chapter.id(), chapter) != null) {
                    throw new IllegalStateException("Duplicate chapter id " + chapter.id() + " in " + chapterDir);
                }
                chapterIds.add(chapter.id());
            }
            units.add(new Series.Unit(uf.id(), uf.number() == null ? units.size() : uf.number(), uf.title(),
                    uf.summary(), chapterIds));
        }
        return new Series(sf.id(), sf.title(), sf.tagline(), units);
    }

    private Chapter loadChapter(Path dir, String seriesId, String unitId) {
        try {
            ChapterFile cf = read(dir.resolve("chapter.yaml"), ChapterFile.class);
            ChapterMeta meta = new ChapterMeta(cf.id(), seriesId, unitId, cf.lesson() == null ? 0 : cf.lesson(),
                    cf.title(), cf.subtitle(), cf.minutes() == null ? 20 : cf.minutes(),
                    cf.completionXp() == null ? 10 : cf.completionXp(), nonNull(cf.whereAsked()),
                    nonNull(cf.prerequisites()), nonNull(cf.codeFiles()));

            List<Teaser> teasers = new ArrayList<>();
            Path teasersFile = dir.resolve("teasers.yaml");
            if (Files.exists(teasersFile)) {
                for (TeaserFile t : nonNull(read(teasersFile, TeasersFile.class).teasers())) {
                    teasers.add(new Teaser(t.id(), t.prompt(), t.code(), nonNull(t.options()),
                            t.answer() == null ? -1 : t.answer(), t.reveal(), t.xp() == null ? 1 : t.xp()));
                }
            }

            List<Question> questions = new ArrayList<>();
            List<Flashcard> flashcards = new ArrayList<>();
            Path questionsFile = dir.resolve("questions.yaml");
            if (Files.exists(questionsFile)) {
                QuestionsFile qf = read(questionsFile, QuestionsFile.class);
                nonNull(qf.questions()).forEach(q -> questions.add(QuestionFactory.create(q)));
                for (FlashcardFile f : nonNull(qf.flashcards())) flashcards.add(new Flashcard(f.id(), f.front(), f.back()));
            }
            return new Chapter(meta, teasers, questions, flashcards);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid chapter in " + dir + ": " + e.getMessage(), e);
        }
    }

    private <T> T read(Path file, Class<T> type) {
        try {
            return yaml.readValue(file.toFile(), type);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Cannot parse " + file + ": " + e.getMessage(), e);
        }
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
