package dev.stackcraft.api.progress;

import dev.stackcraft.api.content.adapter.fs.FileSystemContentSource;
import dev.stackcraft.api.progress.application.AwardXpUseCase;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestCard;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestEvent;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestProgress;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestTrap;
import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.domain.XpKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportGuestProgressUseCaseTest {

    private static final String CH = "demo.u00.l01";
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);
    private final InMemoryProgressStore store = new InMemoryProgressStore();
    private final ImportGuestProgressUseCase importer = new ImportGuestProgressUseCase(
            new FileSystemContentSource(Path.of("src/test/resources/content-fixture"), false, clock),
            new AwardXpUseCase(store, store, clock), store, store, clock);

    private static GuestEvent ev(String attempt, XpKind kind, String item, double score, String at) {
        return new GuestEvent(attempt, kind, CH, item, score, Instant.parse(at));
    }

    @Test
    void reAwardsFromContentKeepsTimestampsAndIgnoresJunk() {
        var guest = new GuestProgress(List.of(
                ev("attempt-0001", XpKind.ANSWER, "w1", 1.0, "2026-11-18T09:00:00Z"),     // mcq worth 2
                ev("attempt-0002", XpKind.ANSWER, "k1", 0.8, "2026-11-18T09:05:00Z"),     // problem 8 * 0.8 = 6
                ev("attempt-0003", XpKind.CHAPTER, "complete", 1.0, "2026-11-18T09:10:00Z"), // 10
                ev("attempt-0004", XpKind.ANSWER, "does-not-exist", 1.0, "2026-11-18T09:00:00Z"),
                ev("attempt-0005", XpKind.ANSWER, "i1", 1.0, "2030-01-01T00:00:00Z"),     // future
                ev("bad", XpKind.ANSWER, "i1", 1.0, "2026-11-18T09:00:00Z"),               // unsafe attempt id
                ev("attempt-0006", XpKind.ANSWER, "w1", 1.0, "2026-11-18T09:20:00Z")),    // farming: 0 more XP
                List.of(new GuestTrap(CH, "t1", "teaser", "Does HashSet keep order?", 2, Instant.parse("2026-11-18T09:00:00Z"))),
                List.of(new GuestCard(CH + "/f1", 3, LocalDate.parse("2026-11-22")), new GuestCard("nope/f9", 2, LocalDate.parse("2026-11-22"))));

        var r = importer.importGuest("user-1", guest);

        assertThat(r.xpAwarded()).isEqualTo(18);
        assertThat(r.eventsImported()).isEqualTo(4);
        assertThat(r.eventsSkipped()).isEqualTo(3);
        assertThat(store.events("user-1")).extracting(XpEvent::occurredAt).contains(Instant.parse("2026-11-18T09:00:00Z"));
        assertThat(store.openTraps("user-1")).singleElement().satisfies(t -> assertThat(t.timesFallen()).isEqualTo(2));
        assertThat(store.flashcards("user-1")).singleElement().satisfies(c -> assertThat(c.box()).isEqualTo(3));
    }

    @Test
    void importingTheSameDataTwiceChangesNothing() {
        var guest = new GuestProgress(List.of(ev("attempt-1001", XpKind.ANSWER, "w1", 1.0, "2026-11-18T09:00:00Z")), null, null);
        assertThat(importer.importGuest("user-2", guest).xpAwarded()).isEqualTo(2);

        var again = importer.importGuest("user-2", guest);
        assertThat(again.xpAwarded()).isZero();
        assertThat(again.eventsSkipped()).isEqualTo(1);
        assertThat(store.events("user-2")).hasSize(1);
    }

    @Test
    void clientScoresAreClampedAndClientXpDoesNotExist() {
        var guest = new GuestProgress(List.of(ev("attempt-2001", XpKind.ANSWER, "k1", 50.0, "2026-11-18T09:00:00Z")), null, null);
        assertThat(importer.importGuest("user-3", guest).xpAwarded()).isEqualTo(8); // capped at the content's 8 XP
    }
}
