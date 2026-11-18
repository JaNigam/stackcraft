package dev.stackcraft.api.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A learner account. {@code id} is also the {@code learner_id} on every progress row, so progress code never needs to
 * know accounts exist. {@code passwordHash} is null for future OAuth-only accounts.
 */
public record UserAccount(UUID id, Email email, DisplayName displayName, String passwordHash, Instant createdAt,
                          boolean leaderboardVisible) {

    public static UserAccount register(Email email, DisplayName name, String passwordHash, Instant now) {
        return new UserAccount(UUID.randomUUID(), email, name, passwordHash, now, true);
    }

    public UserAccount withProfile(DisplayName name, boolean visible) {
        return new UserAccount(id, email, name, passwordHash, createdAt, visible);
    }

    public String learnerId() {
        return id.toString();
    }
}
