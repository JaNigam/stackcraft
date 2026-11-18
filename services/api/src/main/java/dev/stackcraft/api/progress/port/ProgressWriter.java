package dev.stackcraft.api.progress.port;

import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.progress.domain.XpEvent;

/** Write side of progress storage. */
public interface ProgressWriter {

    /** @throws DuplicateAttemptException if an event with the same attemptId already exists */
    void append(XpEvent event);

    void saveTrap(TrapEntry entry);

    void saveFlashcard(FlashcardState state);
}
