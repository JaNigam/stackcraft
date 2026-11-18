package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.domain.Email;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.LoginThrottle;
import dev.stackcraft.api.identity.port.PasswordHasher;
import dev.stackcraft.api.identity.port.UserAccounts;

import java.util.Optional;

/**
 * Email + password sign-in. Three defences:
 * <ul>
 *   <li>one error for every wrong combination (no account enumeration);</li>
 *   <li>an unknown email still runs a BCrypt check against a dummy hash, so response time does not tell either;</li>
 *   <li>the throttle stops online guessing (5 failures per email and client per window).</li>
 * </ul>
 */
public class LoginUseCase {

    private final UserAccounts accounts;
    private final PasswordHasher hasher;
    private final LoginThrottle throttle;
    private final SessionIssuer sessions;
    private final String dummyHash;

    public LoginUseCase(UserAccounts accounts, PasswordHasher hasher, LoginThrottle throttle, SessionIssuer sessions) {
        this.accounts = accounts;
        this.hasher = hasher;
        this.throttle = throttle;
        this.sessions = sessions;
        this.dummyHash = hasher.hash("timing-equaliser-not-a-real-password");
    }

    public Session login(String email, String password, String clientKey) {
        Email e;
        try {
            e = new Email(email);
        } catch (IdentityException.InvalidInput bad) {
            throw new IdentityException.InvalidCredentials();
        }
        String key = e.value() + "|" + clientKey;
        if (!throttle.allowed(key)) throw new IdentityException.TooManyAttempts();

        Optional<UserAccount> user = accounts.byEmail(e);
        String hash = user.map(UserAccount::passwordHash).filter(h -> h != null).orElse(dummyHash);
        boolean ok = password != null && hasher.matches(password, hash) && user.isPresent() && user.get().passwordHash() != null;
        if (!ok) {
            throttle.recordFailure(key);
            throw new IdentityException.InvalidCredentials();
        }
        throttle.reset(key);
        return sessions.start(user.get());
    }
}
