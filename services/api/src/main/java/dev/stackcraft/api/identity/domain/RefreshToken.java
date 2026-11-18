package dev.stackcraft.api.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side record of one refresh token. Only the SHA-256 hash is kept, so a database leak does not leak sessions.
 * Every token belongs to a family (one sign-in); rotation keeps the family, reuse revokes it.
 */
public record RefreshToken(UUID id, UUID userId, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt,
                           Instant revokedAt, UUID replacedById) {

    public static RefreshToken issue(UUID userId, UUID familyId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, familyId, tokenHash, now, expiresAt, null, null);
    }

    /** Usable right now: not expired, not revoked, not already rotated. */
    public boolean isActive(Instant now) {
        return revokedAt == null && replacedById == null && now.isBefore(expiresAt);
    }

    /** Presented again after it was rotated or revoked: someone is replaying an old token. */
    public boolean isReuse() {
        return revokedAt != null || replacedById != null;
    }

}
