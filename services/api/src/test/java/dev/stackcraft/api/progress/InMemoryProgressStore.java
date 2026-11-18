package dev.stackcraft.api.progress;

import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.port.DuplicateAttemptException;
import dev.stackcraft.api.progress.port.ProgressStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double for the port. Also proves the use cases need nothing from JPA. */
public class InMemoryProgressStore implements ProgressStore {

    private final List<XpEvent> events = new ArrayList<>();
    private final Map<String, TrapEntry> traps = new LinkedHashMap<>();
    private final Map<String, FlashcardState> cards = new LinkedHashMap<>();

    @Override
    public synchronized List<XpEvent> events(String learnerId) {
        return events.stream().filter(e -> e.learnerId().equals(learnerId)).toList();
    }

    @Override
    public synchronized Optional<XpEvent> eventByAttempt(String attemptId) {
        return events.stream().filter(e -> e.attemptId().equals(attemptId)).findFirst();
    }

    @Override
    public synchronized Optional<TrapEntry> trap(String learnerId, String chapterId, String itemId) {
        return Optional.ofNullable(traps.get(learnerId + chapterId + itemId));
    }

    @Override
    public synchronized List<TrapEntry> openTraps(String learnerId) {
        return traps.values().stream().filter(t -> t.learnerId().equals(learnerId) && !t.cleared()).toList();
    }

    @Override
    public synchronized List<FlashcardState> flashcards(String learnerId) {
        return cards.values().stream().filter(c -> c.learnerId().equals(learnerId)).toList();
    }

    @Override
    public synchronized Optional<FlashcardState> flashcard(String learnerId, String cardKey) {
        return Optional.ofNullable(cards.get(learnerId + cardKey));
    }

    @Override
    public synchronized void append(XpEvent event) {
        if (eventByAttempt(event.attemptId()).isPresent()) throw new DuplicateAttemptException(event.attemptId());
        events.add(event);
    }

    @Override
    public synchronized void saveTrap(TrapEntry entry) {
        traps.put(entry.learnerId() + entry.chapterId() + entry.itemId(), entry);
    }

    @Override
    public synchronized void saveFlashcard(FlashcardState state) {
        cards.put(state.learnerId() + state.cardKey(), state);
    }
}
