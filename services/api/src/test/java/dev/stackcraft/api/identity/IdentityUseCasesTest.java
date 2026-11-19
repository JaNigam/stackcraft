package dev.stackcraft.api.identity;

import dev.stackcraft.api.identity.adapter.InMemoryLoginThrottle;
import dev.stackcraft.api.identity.application.LoginUseCase;
import dev.stackcraft.api.identity.application.LogoutUseCase;
import dev.stackcraft.api.identity.application.OpaqueTokens;
import dev.stackcraft.api.identity.application.ProfileUseCase;
import dev.stackcraft.api.identity.application.RefreshSessionUseCase;
import dev.stackcraft.api.identity.application.RegisterUseCase;
import dev.stackcraft.api.identity.application.Session;
import dev.stackcraft.api.identity.application.SessionIssuer;
import dev.stackcraft.api.identity.domain.Email;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.domain.RefreshToken;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.AccessTokenIssuer;
import dev.stackcraft.api.identity.port.PasswordHasher;
import dev.stackcraft.api.identity.port.RefreshTokens;
import dev.stackcraft.api.identity.port.UserAccounts;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The session rules with in-memory ports: fast, and proves the use cases need nothing from Spring. */
class IdentityUseCasesTest {

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-19T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    static final class Store implements UserAccounts, RefreshTokens {
        final Map<UUID, UserAccount> users = new HashMap<>();
        final Map<UUID, RefreshToken> tokens = new HashMap<>();

        @Override public Optional<UserAccount> byEmail(Email email) { return users.values().stream().filter(u -> u.email().equals(email)).findFirst(); }
        @Override public Optional<UserAccount> byId(UUID id) { return Optional.ofNullable(users.get(id)); }
        @Override public UserAccount save(UserAccount a) {
            if (users.values().stream().anyMatch(u -> u.email().equals(a.email()) && !u.id().equals(a.id()))) throw new IdentityException.EmailTaken();
            users.put(a.id(), a);
            return a;
        }
        @Override public void save(RefreshToken t) { tokens.put(t.id(), t); }
        @Override public Optional<RefreshToken> byHash(String h) { return tokens.values().stream().filter(t -> t.tokenHash().equals(h)).findFirst(); }
        @Override public synchronized boolean markRotated(UUID id, UUID next) {
            RefreshToken t = tokens.get(id);
            if (t == null || t.replacedById() != null || t.revokedAt() != null) return false;
            tokens.put(id, new RefreshToken(t.id(), t.userId(), t.familyId(), t.tokenHash(), t.createdAt(), t.expiresAt(), null, next));
            return true;
        }
        @Override public void revokeFamily(UUID family, Instant at) { tokens.replaceAll((k, t) -> t.familyId().equals(family) && t.revokedAt() == null ? revoked(t, at) : t); }
        @Override public void revokeAllForUser(UUID user, Instant at) { tokens.replaceAll((k, t) -> t.userId().equals(user) && t.revokedAt() == null ? revoked(t, at) : t); }
        private static RefreshToken revoked(RefreshToken t, Instant at) {
            return new RefreshToken(t.id(), t.userId(), t.familyId(), t.tokenHash(), t.createdAt(), t.expiresAt(), at, t.replacedById());
        }
    }

    final MutableClock clock = new MutableClock();
    final Store store = new Store();
    final PasswordHasher hasher = new PasswordHasher() {
        @Override public String hash(String raw) { return "h:" + raw; }
        @Override public boolean matches(String raw, String hash) { return hash.equals("h:" + raw); }
    };
    final AccessTokenIssuer access = user -> new AccessTokenIssuer.AccessToken("jwt-for-" + user.id(), clock.instant().plus(Duration.ofMinutes(10)));
    final SessionIssuer sessions = new SessionIssuer(access, store, clock, Duration.ofDays(30));
    final RegisterUseCase register = new RegisterUseCase(store, hasher, sessions, clock);
    final LoginUseCase login = new LoginUseCase(store, hasher, new InMemoryLoginThrottle(5, Duration.ofMinutes(15), clock), sessions);
    final RefreshSessionUseCase refresh = new RefreshSessionUseCase(store, store, sessions, clock);
    final LogoutUseCase logout = new LogoutUseCase(store, clock);

    private Session ana() {
        return register.register("  Ana@Example.com ", "correct horse battery", "Ana");
    }

    @Test
    void registerNormalisesEmailAndRejectsWeakOrDuplicate() {
        Session s = ana();
        assertThat(s.user().email().value()).isEqualTo("ana@example.com");
        assertThat(s.user().passwordHash()).isEqualTo("h:correct horse battery");
        assertThat(store.tokens.values()).singleElement().satisfies(t -> assertThat(t.tokenHash()).isEqualTo(OpaqueTokens.hash(s.refreshToken())));

        assertThatThrownBy(() -> register.register("ana@example.com", "another long one", "Ana 2")).isInstanceOf(IdentityException.EmailTaken.class);
        assertThatThrownBy(() -> register.register("bo@example.com", "short", "Bo")).isInstanceOf(IdentityException.InvalidInput.class).hasMessageContaining("10");
        assertThatThrownBy(() -> register.register("bo@example.com", "bo@example.com", "Bo")).isInstanceOf(IdentityException.InvalidInput.class);
        assertThatThrownBy(() -> register.register("not-an-email", "correct horse battery", "Bo")).isInstanceOf(IdentityException.InvalidInput.class);
        assertThatThrownBy(() -> register.register("bo@example.com", "correct horse battery", "<script>")).isInstanceOf(IdentityException.InvalidInput.class);
    }

    @Test
    void loginGivesTheSameErrorForUnknownEmailAndWrongPassword() {
        ana();
        assertThat(login.login("ANA@example.com", "correct horse battery", "1.2.3.4").user().displayName().value()).isEqualTo("Ana");
        assertThatThrownBy(() -> login.login("ana@example.com", "wrong password!", "1.2.3.4"))
                .isInstanceOf(IdentityException.InvalidCredentials.class).hasMessage("Email or password is wrong");
        assertThatThrownBy(() -> login.login("nobody@example.com", "whatever12345", "1.2.3.4"))
                .isInstanceOf(IdentityException.InvalidCredentials.class).hasMessage("Email or password is wrong");
    }

    @Test
    void fiveFailuresLockTheKeyEvenForTheRightPasswordUntilTheWindowPasses() {
        ana();
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> login.login("ana@example.com", "wrong password!", "1.2.3.4")).isInstanceOf(IdentityException.InvalidCredentials.class);
        }
        assertThatThrownBy(() -> login.login("ana@example.com", "correct horse battery", "1.2.3.4")).isInstanceOf(IdentityException.TooManyAttempts.class);
        assertThat(login.login("ana@example.com", "correct horse battery", "5.6.7.8")).isNotNull(); // another client is not locked
        clock.advance(Duration.ofMinutes(16));
        assertThat(login.login("ana@example.com", "correct horse battery", "1.2.3.4")).isNotNull();
    }

    @Test
    void refreshRotatesTheTokenAndTheOldOneStopsWorking() {
        Session first = ana();
        Session second = refresh.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.user().id()).isEqualTo(first.user().id());
        assertThat(refresh.refresh(second.refreshToken())).isNotNull();
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        Session first = ana();
        Session second = refresh.refresh(first.refreshToken());

        // An attacker replays the first token: rejected, and the legitimate newer token dies with it.
        assertThatThrownBy(() -> refresh.refresh(first.refreshToken())).isInstanceOf(IdentityException.InvalidSession.class);
        assertThatThrownBy(() -> refresh.refresh(second.refreshToken())).isInstanceOf(IdentityException.InvalidSession.class);
    }

    @Test
    void expiredAndUnknownTokensAreRejected() {
        Session s = ana();
        assertThatThrownBy(() -> refresh.refresh("made-up-token")).isInstanceOf(IdentityException.InvalidSession.class);
        assertThatThrownBy(() -> refresh.refresh(null)).isInstanceOf(IdentityException.InvalidSession.class);
        clock.advance(Duration.ofDays(31));
        assertThatThrownBy(() -> refresh.refresh(s.refreshToken())).isInstanceOf(IdentityException.InvalidSession.class);
    }

    @Test
    void logoutEndsThisSessionOnlyAndLogoutEverywhereEndsAll() {
        Session laptop = ana();
        Session phone = login.login("ana@example.com", "correct horse battery", "9.9.9.9");

        logout.logout(laptop.refreshToken());
        assertThatThrownBy(() -> refresh.refresh(laptop.refreshToken())).isInstanceOf(IdentityException.InvalidSession.class);
        Session phone2 = refresh.refresh(phone.refreshToken());

        logout.logoutEverywhere(phone2.user().id());
        assertThatThrownBy(() -> refresh.refresh(phone2.refreshToken())).isInstanceOf(IdentityException.InvalidSession.class);
        logout.logout(null); // idempotent, no error
    }

    @Test
    void profileUpdatesNameAndVisibility() {
        UUID id = ana().user().id();
        ProfileUseCase profile = new ProfileUseCase(store);
        assertThat(profile.update(id, "Ana B", false)).satisfies(u -> {
            assertThat(u.displayName().value()).isEqualTo("Ana B");
            assertThat(u.leaderboardVisible()).isFalse();
        });
        assertThat(profile.update(id, null, null).displayName().value()).isEqualTo("Ana B");
    }
}
