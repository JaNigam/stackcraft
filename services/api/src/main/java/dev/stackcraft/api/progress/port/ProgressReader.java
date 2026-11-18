package dev.stackcraft.api.progress.port;

import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.progress.domain.XpEvent;

import java.util.List;
import java.util.Optional;

/** Read side of progress storage (ISP: use cases that only read never see write methods). */
public interface ProgressReader {

    List<XpEvent> events(String learnerId);

    Optional<XpEvent> eventByAttempt(String attemptId);

    Optional<TrapEntry> trap(String learnerId, String chapterId, String itemId);

    List<TrapEntry> openTraps(String learnerId);

    List<FlashcardState> flashcards(String learnerId);

    Optional<FlashcardState> flashcard(String learnerId, String cardKey);
}
