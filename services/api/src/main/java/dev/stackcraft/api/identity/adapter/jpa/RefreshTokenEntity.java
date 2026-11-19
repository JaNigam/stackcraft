package dev.stackcraft.api.identity.adapter.jpa;

import dev.stackcraft.api.identity.domain.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
class RefreshTokenEntity {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "family_id", nullable = false)
    UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    String tokenHash;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    @Column(name = "replaced_by_id")
    UUID replacedById;

    protected RefreshTokenEntity() {}

    static RefreshTokenEntity from(RefreshToken t) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.id = t.id();
        e.userId = t.userId();
        e.familyId = t.familyId();
        e.tokenHash = t.tokenHash();
        e.createdAt = t.createdAt();
        e.expiresAt = t.expiresAt();
        e.revokedAt = t.revokedAt();
        e.replacedById = t.replacedById();
        return e;
    }

    RefreshToken toDomain() {
        return new RefreshToken(id, userId, familyId, tokenHash, createdAt, expiresAt, revokedAt, replacedById);
    }
}
