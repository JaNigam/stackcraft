package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.domain.RefreshToken;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.RefreshTokens;
import dev.stackcraft.api.identity.port.UserAccounts;

import java.time.Clock;
import java.time.Instant;

/**
 * Refresh-token rotation with reuse detection.
 * <ol>
 *   <li>Unknown or expired token: the session is over.</li>
 *   <li>Token already rotated or revoked: someone replayed an old token (maybe stolen). Revoke the whole family, so
 *       the thief and the real user are both signed out and the real user signs in again.</li>
 *   <li>Otherwise: link the old token to a new one in the same family and hand out the new one.</li>
 * </ol>
 */
public class RefreshSessionUseCase {

    private final RefreshTokens tokens;
    private final UserAccounts accounts;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RefreshSessionUseCase(RefreshTokens tokens, UserAccounts accounts, SessionIssuer sessions, Clock clock) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.sessions = sessions;
        this.clock = clock;
    }

    public Session refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new IdentityException.InvalidSession();
        Instant now = clock.instant();
        RefreshToken current = tokens.byHash(OpaqueTokens.hash(rawToken)).orElseThrow(IdentityException.InvalidSession::new);
        if (current.isReuse()) {
            tokens.revokeFamily(current.familyId(), now);
            throw new IdentityException.InvalidSession();
        }
        if (!current.isActive(now)) throw new IdentityException.InvalidSession();

        UserAccount user = accounts.byId(current.userId()).orElseThrow(IdentityException.InvalidSession::new);
        SessionIssuer.Issued next = sessions.issue(user, current.familyId());
        // Compare-and-set: only one concurrent refresh with the same token can win. The loser is a replay.
        if (!tokens.markRotated(current.id(), next.refreshTokenId())) {
            tokens.revokeFamily(current.familyId(), now);
            throw new IdentityException.InvalidSession();
        }
        return next.session();
    }
}
