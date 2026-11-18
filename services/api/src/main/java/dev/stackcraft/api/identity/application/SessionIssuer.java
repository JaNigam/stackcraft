package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.domain.RefreshToken;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.AccessTokenIssuer;
import dev.stackcraft.api.identity.port.RefreshTokens;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Issues an access token plus a refresh token in a given family. Shared by login, register and refresh. */
public class SessionIssuer {

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokens refreshTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    public SessionIssuer(AccessTokenIssuer accessTokens, RefreshTokens refreshTokens, Clock clock, Duration refreshTtl) {
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    /** A brand new sign-in: new family. */
    public Session start(UserAccount user) {
        return issue(user, UUID.randomUUID()).session();
    }

    /** Next token in an existing family; returns the new token's id so the caller can link the old one to it. */
    Issued issue(UserAccount user, UUID familyId) {
        Instant now = clock.instant();
        String raw = OpaqueTokens.newToken();
        RefreshToken stored = RefreshToken.issue(user.id(), familyId, OpaqueTokens.hash(raw), now, now.plus(refreshTtl));
        refreshTokens.save(stored);
        AccessTokenIssuer.AccessToken access = accessTokens.issue(user);
        return new Issued(new Session(user, access.value(), access.expiresAt(), raw, stored.expiresAt()), stored.id());
    }

    record Issued(Session session, UUID refreshTokenId) {}
}
