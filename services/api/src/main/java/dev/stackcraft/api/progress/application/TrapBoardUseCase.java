package dev.stackcraft.api.progress.application;

import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.progress.port.ProgressReader;
import dev.stackcraft.api.progress.port.ProgressWriter;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

/** Records falls and clears, and lists what is still open (most-fallen first). */
public class TrapBoardUseCase {

    private final ProgressReader reader;
    private final ProgressWriter writer;
    private final Clock clock;

    public TrapBoardUseCase(ProgressReader reader, ProgressWriter writer, Clock clock) {
        this.reader = reader;
        this.writer = writer;
        this.clock = clock;
    }

    public void fell(String learnerId, String chapterId, String itemId, String kind, String prompt) {
        TrapEntry next = reader.trap(learnerId, chapterId, itemId)
                .map(t -> t.fallenAgain(clock.instant()))
                .orElseGet(() -> TrapEntry.firstFall(learnerId, chapterId, itemId, kind, shorten(prompt), clock.instant()));
        writer.saveTrap(next);
    }

    public void escaped(String learnerId, String chapterId, String itemId) {
        reader.trap(learnerId, chapterId, itemId).filter(t -> !t.cleared()).ifPresent(t -> writer.saveTrap(t.markCleared()));
    }

    public List<TrapEntry> open(String learnerId) {
        return reader.openTraps(learnerId).stream()
                .sorted(Comparator.comparingInt(TrapEntry::timesFallen).reversed()
                        .thenComparing(TrapEntry::lastFallenAt, Comparator.reverseOrder()))
                .toList();
    }

    private static String shorten(String prompt) {
        if (prompt == null) return "";
        String oneLine = prompt.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 297) + "...";
    }
}
