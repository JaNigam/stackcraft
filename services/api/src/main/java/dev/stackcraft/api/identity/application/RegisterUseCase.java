package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.domain.DisplayName;
import dev.stackcraft.api.identity.domain.Email;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.domain.PasswordPolicy;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.PasswordHasher;
import dev.stackcraft.api.identity.port.UserAccounts;

import java.time.Clock;

/** Create an account and sign it in. */
public class RegisterUseCase {

    private final UserAccounts accounts;
    private final PasswordHasher hasher;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RegisterUseCase(UserAccounts accounts, PasswordHasher hasher, SessionIssuer sessions, Clock clock) {
        this.accounts = accounts;
        this.hasher = hasher;
        this.sessions = sessions;
        this.clock = clock;
    }

    public Session register(String email, String password, String displayName) {
        Email e = new Email(email);
        DisplayName name = new DisplayName(displayName);
        PasswordPolicy.check(password, e);
        if (accounts.byEmail(e).isPresent()) throw new IdentityException.EmailTaken();
        UserAccount saved = accounts.save(UserAccount.register(e, name, hasher.hash(password), clock.instant()));
        return sessions.start(saved);
    }
}
