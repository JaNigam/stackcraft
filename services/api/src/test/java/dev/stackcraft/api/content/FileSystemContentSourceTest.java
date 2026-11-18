package dev.stackcraft.api.content;

import dev.stackcraft.api.content.adapter.fs.FileSystemContentSource;
import dev.stackcraft.api.quiz.domain.Question;
import dev.stackcraft.api.quiz.domain.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemContentSourceTest {

    private static final Path FIXTURE = Path.of("src/test/resources/content-fixture");

    @Test
    void loadsSeriesChaptersAndEveryQuestionType() {
        var source = new FileSystemContentSource(FIXTURE, false, Clock.systemUTC());

        assertThat(source.allSeries()).singleElement().satisfies(s -> {
            assertThat(s.id()).isEqualTo("demo");
            assertThat(s.units().getFirst().chapterIds()).containsExactly("demo.u00.l01");
        });
        var chapter = source.chapter("demo.u00.l01").orElseThrow();
        assertThat(chapter.meta().seriesId()).isEqualTo("demo");
        assertThat(chapter.teasers()).hasSize(1);
        assertThat(chapter.flashcards()).hasSize(1);
        assertThat(chapter.questions()).extracting(q -> q.getClass().getSimpleName())
                .containsExactly("Mcq", "PredictOutput", "ShortAnswer", "Problem");
        assertThat(chapter.question("w1").orElseThrow().xp()).isEqualTo(Tier.WARMUP.defaultXp());
        assertThat(((Question.PredictOutput) chapter.question("i1").orElseThrow()).expected()).isEqualTo("33");
    }

    @Test
    void brokenContentFailsLoudlyWithTheFolderName() {
        assertThatThrownBy(() -> new FileSystemContentSource(Path.of("src/test/resources/content-broken"), false, Clock.systemUTC()))
                .hasMessageContaining("l01")
                .hasMessageContaining("answer index out of range");
    }

    @Test
    void reloadPicksUpEditsAndKeepsLastGoodVersionOnBrokenEdit(@TempDir Path temp) throws IOException {
        copy(FIXTURE, temp);
        AtomicLong now = new AtomicLong(0);
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return Instant.ofEpochMilli(now.get()); }
        };
        var source = new FileSystemContentSource(temp, true, clock);
        Path chapterYaml = temp.resolve("demo/u00-intro/l01-basics/chapter.yaml");

        Files.writeString(chapterYaml, Files.readString(chapterYaml).replace("title: Basics", "title: Basics v2"));
        now.addAndGet(1500);
        assertThat(source.chapter("demo.u00.l01").orElseThrow().meta().title()).isEqualTo("Basics v2");

        Files.writeString(temp.resolve("demo/u00-intro/l01-basics/questions.yaml"), "questions: [ {id: x, type: nope, tier: warmup} ]");
        now.addAndGet(1500);
        assertThat(source.chapter("demo.u00.l01").orElseThrow().meta().title()).isEqualTo("Basics v2");
        assertThat(source.chapter("demo.u00.l01").orElseThrow().questions()).hasSize(4);
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path p : paths.toList()) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target);
            }
        }
    }
}
