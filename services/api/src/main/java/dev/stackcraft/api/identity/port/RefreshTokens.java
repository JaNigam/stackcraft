package dev.stackcraft.api.identity.port;

import dev.stackcraft.api.identity.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokens {

    void save(RefreshToken token);

    Optional<RefreshToken> byHash(String tokenHash);

    /**
     * Atomically links {@code id} to its successor, only if it is still unrotated and unrevoked.
     * @return false if another request rotated or revoked it first
     */
    boolean markRotated(UUID id, UUID replacedById);

    void revokeFamily(UUID familyId, Instant at);

    void revokeAllForUser(UUID userId, Instant at);
}
