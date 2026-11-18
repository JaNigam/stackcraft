package dev.stackcraft.api.progress.adapter.jpa;

import dev.stackcraft.api.progress.domain.FlashcardState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "flashcard_state")
class FlashcardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "learner_id", nullable = false, length = 64)
    String learnerId;

    @Column(name = "card_key", nullable = false, length = 256)
    String cardKey;

    @Column(nullable = false)
    int box;

    @Column(name = "due_on", nullable = false)
    LocalDate dueOn;

    protected FlashcardEntity() {}

    void copyFrom(FlashcardState s) {
        learnerId = s.learnerId();
        cardKey = s.cardKey();
        box = s.box();
        dueOn = s.dueOn();
    }

    static FlashcardEntity from(FlashcardState s) {
        FlashcardEntity e = new FlashcardEntity();
        e.copyFrom(s);
        return e;
    }

    FlashcardState toDomain() {
        return new FlashcardState(learnerId, cardKey, box, dueOn);
    }
}
