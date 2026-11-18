package dev.stackcraft.api.progress.adapter.jpa;

import dev.stackcraft.api.progress.domain.XpEvent;
import dev.stackcraft.api.progress.domain.XpKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persistence shape only. Mapped to and from the domain record at the adapter edge, so JPA never leaks inward. */
@Entity
@Table(name = "xp_event")
class XpEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "attempt_id", nullable = false, unique = true, length = 64)
    String attemptId;

    @Column(name = "learner_id", nullable = false, length = 64)
    String learnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    XpKind kind;

    @Column(name = "chapter_id", nullable = false, length = 128)
    String chapterId;

    @Column(name = "item_id", nullable = false, length = 128)
    String itemId;

    @Column(nullable = false)
    int xp;

    @Column(nullable = false)
    double score;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;

    protected XpEventEntity() {}

    static XpEventEntity from(XpEvent e) {
        XpEventEntity x = new XpEventEntity();
        x.attemptId = e.attemptId();
        x.learnerId = e.learnerId();
        x.kind = e.kind();
        x.chapterId = e.chapterId();
        x.itemId = e.itemId();
        x.xp = e.xp();
        x.score = e.score();
        x.occurredAt = e.occurredAt();
        return x;
    }

    XpEvent toDomain() {
        return new XpEvent(attemptId, learnerId, kind, chapterId, itemId, xp, score, occurredAt);
    }
}
