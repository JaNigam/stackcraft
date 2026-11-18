package dev.stackcraft.api.progress;

import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.LearnerProgress;
import dev.stackcraft.api.progress.domain.Rank;
import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.domain.XpKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressDomainTest {

    private static XpEvent at(String day, int xp) {
        return new XpEvent("a-" + day + xp, "local", XpKind.ANSWER, "c", "q" + day, xp, 1.0, Instant.parse(day + "T12:00:00Z"));
    }

    @Test
    void rankThresholdsMatchTheCourse() {
        assertThat(Rank.of(0)).isEqualTo(Rank.TRAINEE);
        assertThat(Rank.of(199)).isEqualTo(Rank.TRAINEE);
        assertThat(Rank.of(200)).isEqualTo(Rank.DEVELOPER);
        assertThat(Rank.of(500)).isEqualTo(Rank.SENIOR);
        assertThat(Rank.of(900)).isEqualTo(Rank.STAFF);
        assertThat(Rank.of(1300)).isEqualTo(Rank.PRINCIPAL);
        assertThat(Rank.PRINCIPAL.next()).isEmpty();
    }

    @Test
    void progressIsDerivedFromEvents() {
        var p = LearnerProgress.from("local", List.of(at("2026-11-18", 150), at("2026-11-19", 60)),
                LocalDate.parse("2026-11-19"), ZoneOffset.UTC);

        assertThat(p.totalXp()).isEqualTo(210);
        assertThat(p.rank()).isEqualTo(Rank.DEVELOPER);
        assertThat(p.nextRank()).isEqualTo(Rank.SENIOR);
        assertThat(p.xpToNextRank()).isEqualTo(290);
        assertThat(p.streakDays()).isEqualTo(2);
    }

    @Test
    void streakSurvivesUntilMidnightAndBreaksOnAGap() {
        var events = List.of(at("2026-11-15", 1), at("2026-11-17", 1), at("2026-11-18", 1));

        assertThat(LearnerProgress.from("local", events, LocalDate.parse("2026-11-19"), ZoneOffset.UTC).streakDays()).isEqualTo(2);
        assertThat(LearnerProgress.from("local", events, LocalDate.parse("2026-11-20"), ZoneOffset.UTC).streakDays()).isZero();
    }

    @Test
    void leitnerBoxesMoveUpOnRememberAndResetOnForget() {
        LocalDate today = LocalDate.parse("2026-11-19");
        var card = FlashcardState.fresh("local", "c/f1", today);
        assertThat(card.isDue(today)).isTrue();

        var up = card.review(true, today);
        assertThat(up.box()).isEqualTo(2);
        assertThat(up.dueOn()).isEqualTo(today.plusDays(2));
        assertThat(up.isDue(today)).isFalse();

        var top = up.review(true, today).review(true, today).review(true, today).review(true, today);
        assertThat(top.box()).isEqualTo(5);
        assertThat(top.dueOn()).isEqualTo(today.plusDays(16));

        var reset = top.review(false, today);
        assertThat(reset.box()).isEqualTo(1);
        assertThat(reset.dueOn()).isEqualTo(today.plusDays(1));
    }
}
