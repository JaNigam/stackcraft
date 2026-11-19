package dev.stackcraft.api.identity.adapter.web;

import dev.stackcraft.api.identity.domain.UserAccount;

import java.util.UUID;

/** Wire shape of the signed-in learner. Never includes the password hash. */
public record MeView(UUID id, String email, String displayName, boolean leaderboardVisible) {
    static MeView of(UserAccount u) {
        return new MeView(u.id(), u.email().value(), u.displayName().value(), u.leaderboardVisible());
    }
}
