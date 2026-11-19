package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.port.RefreshTokens;

import java.time.Clock;
import java.util.UUID;

/** Logout revokes the refresh family behind this browser; logout-all revokes every family of the user. */
public class LogoutUseCase {

    private final RefreshTokens tokens;
    private final Clock clock;

    public LogoutUseCase(RefreshTokens tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Idempotent: an unknown or missing token is simply nothing to revoke. */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        tokens.byHash(OpaqueTokens.hash(rawRefreshToken)).ifPresent(t -> tokens.revokeFamily(t.familyId(), clock.instant()));
    }

    public void logoutEverywhere(UUID userId) {
        tokens.revokeAllForUser(userId, clock.instant());
    }
}
