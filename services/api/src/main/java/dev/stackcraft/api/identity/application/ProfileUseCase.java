package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.domain.DisplayName;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.UserAccounts;

import java.util.UUID;

/** Read and edit the signed-in learner's own profile. */
public class ProfileUseCase {

    private final UserAccounts accounts;

    public ProfileUseCase(UserAccounts accounts) {
        this.accounts = accounts;
    }

    public UserAccount me(UUID userId) {
        return accounts.byId(userId).orElseThrow(IdentityException.Unauthenticated::new);
    }

    public UserAccount update(UUID userId, String displayName, Boolean leaderboardVisible) {
        UserAccount current = me(userId);
        DisplayName name = displayName == null ? current.displayName() : new DisplayName(displayName);
        boolean visible = leaderboardVisible == null ? current.leaderboardVisible() : leaderboardVisible;
        return accounts.save(current.withProfile(name, visible));
    }
}
