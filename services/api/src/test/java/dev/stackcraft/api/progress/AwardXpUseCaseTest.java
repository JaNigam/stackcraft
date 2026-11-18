package dev.stackcraft.api.progress;

import dev.stackcraft.api.progress.application.AwardXpUseCase;
import dev.stackcraft.api.progress.application.AwardXpUseCase.XpClaim;
import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.domain.XpKind;
import dev.stackcraft.api.progress.port.DuplicateAttemptException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AwardXpUseCaseTest {

    private final InMemoryProgressStore store = new InMemoryProgressStore();
    private final AwardXpUseCase award = new AwardXpUseCase(store, store, Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC));

    private static XpClaim claim(String attemptId, double score) {
        return new XpClaim(attemptId, "local", XpKind.ANSWER, "c1", "q1", 4, score);
    }

    @Test
    void sameAttemptIdTwiceAwardsOnce() {
        var first = award.award(claim("attempt-0001", 1.0));
        var retry = award.award(claim("attempt-0001", 1.0));

        assertThat(first.xpAwarded()).isEqualTo(4);
        assertThat(first.duplicate()).isFalse();
        assertThat(retry.duplicate()).isTrue();
        assertThat(retry.xpAwarded()).isEqualTo(4); // replays the original result
        assertThat(store.events("local")).hasSize(1);
    }

    @Test
    void answeringAgainWithANewAttemptCannotFarmXp() {
        award.award(claim("attempt-0001", 1.0));
        var again = award.award(claim("attempt-0002", 1.0));

        assertThat(again.xpAwarded()).isZero();
        assertThat(store.events("local").stream().mapToInt(XpEvent::xp).sum()).isEqualTo(4);
    }

    @Test
    void improvingAPartialScoreTopsUpToTheMaximum() {
        assertThat(award.award(claim("attempt-0001", 0.5)).xpAwarded()).isEqualTo(2);
        assertThat(award.award(claim("attempt-0002", 1.0)).xpAwarded()).isEqualTo(2);
        assertThat(award.award(claim("attempt-0003", 1.0)).xpAwarded()).isZero();
    }

    @Test
    void lostRaceOnTheUniqueConstraintIsReportedAsDuplicate() {
        // Simulates two identical requests: the other one wrote first, between our check and our insert.
        var racing = new InMemoryProgressStore() {
            boolean first = true;

            @Override
            public synchronized java.util.Optional<XpEvent> eventByAttempt(String attemptId) {
                if (first) { first = false; return java.util.Optional.empty(); }
                return super.eventByAttempt(attemptId);
            }
        };
        racing.append(new XpEvent("attempt-race", "local", XpKind.ANSWER, "c1", "q1", 4, 1.0, Instant.EPOCH));
        var useCase = new AwardXpUseCase(racing, racing, Clock.systemUTC());

        var result = useCase.award(new XpClaim("attempt-race", "local", XpKind.ANSWER, "c1", "q1", 4, 1.0));

        assertThat(result.duplicate()).isTrue();
        assertThat(racing.events("local")).hasSize(1);
    }

    @Test
    void rejectsMissingOrUnsafeAttemptIds() {
        assertThatThrownBy(() -> award.award(claim(null, 1.0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> award.award(claim("short", 1.0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> award.award(claim("../../etc/passwd", 1.0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storeRejectsDuplicateAppend() {
        store.append(new XpEvent("attempt-x001", "local", XpKind.ANSWER, "c", "q", 1, 1, Instant.EPOCH));
        assertThatThrownBy(() -> store.append(new XpEvent("attempt-x001", "local", XpKind.ANSWER, "c", "q", 1, 1, Instant.EPOCH)))
                .isInstanceOf(DuplicateAttemptException.class);
    }
}
