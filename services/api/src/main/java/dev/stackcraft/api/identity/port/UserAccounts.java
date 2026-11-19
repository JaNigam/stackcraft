package dev.stackcraft.api.identity.port;

import dev.stackcraft.api.identity.domain.Email;
import dev.stackcraft.api.identity.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccounts {

    Optional<UserAccount> byEmail(Email email);

    Optional<UserAccount> byId(UUID id);

    /** @throws dev.stackcraft.api.identity.domain.IdentityException.EmailTaken if the email exists (unique index) */
    UserAccount save(UserAccount account);
}
