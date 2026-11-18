package dev.stackcraft.api.progress.adapter.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repositories. Package-private: only {@link JpaProgressStore} may use them. */
final class Repositories {
    private Repositories() {}

    interface XpEvents extends JpaRepository<XpEventEntity, Long> {
        List<XpEventEntity> findByLearnerIdOrderByOccurredAtAsc(String learnerId);

        Optional<XpEventEntity> findByAttemptId(String attemptId);
    }

    interface Traps extends JpaRepository<TrapEntity, Long> {
        Optional<TrapEntity> findByLearnerIdAndChapterIdAndItemId(String learnerId, String chapterId, String itemId);

        List<TrapEntity> findByLearnerIdAndClearedFalse(String learnerId);
    }

    interface Flashcards extends JpaRepository<FlashcardEntity, Long> {
        List<FlashcardEntity> findByLearnerId(String learnerId);

        Optional<FlashcardEntity> findByLearnerIdAndCardKey(String learnerId, String cardKey);
    }
}
