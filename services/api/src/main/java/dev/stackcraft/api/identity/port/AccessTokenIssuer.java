package dev.stackcraft.api.identity.port;

import dev.stackcraft.api.identity.domain.UserAccount;

import java.time.Instant;

/** Port: signs a short-lived access token for a user. The JWT adapter is the only implementation today. */
public interface AccessTokenIssuer {

    AccessToken issue(UserAccount user);

    record AccessToken(String value, Instant expiresAt) {}
}
