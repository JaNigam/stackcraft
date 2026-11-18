package dev.stackcraft.api.progress.adapter.jpa;

import dev.stackcraft.api.progress.domain.TrapEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "trap_entry")
class TrapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "learner_id", nullable = false, length = 64)
    String learnerId;

    @Column(name = "chapter_id", nullable = false, length = 128)
    String chapterId;

    @Column(name = "item_id", nullable = false, length = 128)
    String itemId;

    @Column(nullable = false, length = 16)
    String kind;

    @Column(nullable = false, length = 2000)
    String prompt;

    @Column(name = "times_fallen", nullable = false)
    int timesFallen;

    @Column(name = "last_fallen_at", nullable = false)
    Instant lastFallenAt;

    @Column(nullable = false)
    boolean cleared;

    protected TrapEntity() {}

    void copyFrom(TrapEntry t) {
        learnerId = t.learnerId();
        chapterId = t.chapterId();
        itemId = t.itemId();
        kind = t.kind();
        prompt = t.prompt();
        timesFallen = t.timesFallen();
        lastFallenAt = t.lastFallenAt();
        cleared = t.cleared();
    }

    static TrapEntity from(TrapEntry t) {
        TrapEntity e = new TrapEntity();
        e.copyFrom(t);
        return e;
    }

    TrapEntry toDomain() {
        return new TrapEntry(learnerId, chapterId, itemId, kind, prompt, timesFallen, lastFallenAt, cleared);
    }
}
