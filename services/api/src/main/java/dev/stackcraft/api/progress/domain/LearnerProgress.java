package dev.stackcraft.api.progress.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Read model derived from the XP log. Pure function of (events, today): easy to test, nothing to keep in sync.
 *
 * @param bestScores best score per item, keyed {@code chapterId/itemId}
 */
public record LearnerProgress(String learnerId, int totalXp, Rank rank, Rank nextRank, int xpToNextRank,
                              int streakDays, Set<String> completedChapters, Map<String, Double> bestScores) {

    public static LearnerProgress from(String learnerId, List<XpEvent> events, LocalDate today, ZoneId zone) {
        int total = events.stream().mapToInt(XpEvent::xp).sum();
        Rank rank = Rank.of(total);
        Rank next = rank.next().orElse(null);
        int toNext = next == null ? 0 : next.minXp() - total;

        Set<String> completed = events.stream().filter(e -> e.kind() == XpKind.CHAPTER)
                .map(XpEvent::chapterId).collect(Collectors.toCollection(TreeSet::new));

        Map<String, Double> best = new HashMap<>();
        for (XpEvent e : events) {
            if (e.kind() == XpKind.ANSWER || e.kind() == XpKind.TEASER) {
                best.merge(e.chapterId() + "/" + e.itemId(), e.score(), Math::max);
            }
        }
        return new LearnerProgress(learnerId, total, rank, next, toNext, streak(events, today, zone), completed, best);
    }

    /**
     * Consecutive days with at least one event, counting back from today. If today has nothing yet but yesterday
     * does, the streak is still alive (you have until midnight).
     */
    static int streak(List<XpEvent> events, LocalDate today, ZoneId zone) {
        Set<LocalDate> days = events.stream().map(e -> LocalDate.ofInstant(e.occurredAt(), zone)).collect(Collectors.toSet());
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }
}
