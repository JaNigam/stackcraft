package dev.stackcraft.api.progress.adapter.jpa;

import dev.stackcraft.api.progress.domain.FlashcardState;
import dev.stackcraft.api.progress.domain.TrapEntry;
import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.port.DuplicateAttemptException;
import dev.stackcraft.api.progress.port.ProgressStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adapter: progress in a relational DB (H2 in dev, Postgres in prod), schema owned by Flyway. */
public class JpaProgressStore implements ProgressStore {

    private final Repositories.XpEvents events;
    private final Repositories.Traps traps;
    private final Repositories.Flashcards cards;

    JpaProgressStore(Repositories.XpEvents events, Repositories.Traps traps, Repositories.Flashcards cards) {
        this.events = events;
        this.traps = traps;
        this.cards = cards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<XpEvent> events(String learnerId) {
        return events.findByLearnerIdOrderByOccurredAtAsc(learnerId).stream().map(XpEventEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<XpEvent> eventByAttempt(String attemptId) {
        return events.findByAttemptId(attemptId).map(XpEventEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrapEntry> trap(String learnerId, String chapterId, String itemId) {
        return traps.findByLearnerIdAndChapterIdAndItemId(learnerId, chapterId, itemId).map(TrapEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrapEntry> openTraps(String learnerId) {
        return traps.findByLearnerIdAndClearedFalse(learnerId).stream().map(TrapEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardState> flashcards(String learnerId) {
        return cards.findByLearnerId(learnerId).stream().map(FlashcardEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FlashcardState> flashcard(String learnerId, String cardKey) {
        return cards.findByLearnerIdAndCardKey(learnerId, cardKey).map(FlashcardEntity::toDomain);
    }

    /** The unique index on attempt_id is the real idempotency guard; this just translates its error. */
    @Override
    public void append(XpEvent event) {
        try {
            events.saveAndFlush(XpEventEntity.from(event));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateAttemptException(event.attemptId());
        }
    }

    @Override
    @Transactional
    public void saveTrap(TrapEntry entry) {
        TrapEntity entity = traps.findByLearnerIdAndChapterIdAndItemId(entry.learnerId(), entry.chapterId(), entry.itemId())
                .orElseGet(() -> TrapEntity.from(entry));
        entity.copyFrom(entry);
        traps.save(entity);
    }

    @Override
    @Transactional
    public void saveFlashcard(FlashcardState state) {
        FlashcardEntity entity = cards.findByLearnerIdAndCardKey(state.learnerId(), state.cardKey())
                .orElseGet(() -> FlashcardEntity.from(state));
        entity.copyFrom(state);
        cards.save(entity);
    }
}
