package dev.stackcraft.api.identity;

import dev.stackcraft.api.identity.adapter.InMemoryLoginThrottle;
import dev.stackcraft.api.identity.adapter.web.SessionCookies;
import dev.stackcraft.api.identity.application.LoginUseCase;
import dev.stackcraft.api.identity.application.LogoutUseCase;
import dev.stackcraft.api.identity.application.ProfileUseCase;
import dev.stackcraft.api.identity.application.RefreshSessionUseCase;
import dev.stackcraft.api.identity.application.RegisterUseCase;
import dev.stackcraft.api.identity.application.SessionIssuer;
import dev.stackcraft.api.identity.port.AccessTokenIssuer;
import dev.stackcraft.api.identity.port.LoginThrottle;
import dev.stackcraft.api.identity.port.PasswordHasher;
import dev.stackcraft.api.identity.port.RefreshTokens;
import dev.stackcraft.api.identity.port.UserAccounts;
import dev.stackcraft.api.shared.StackcraftProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** Composition root for accounts and sessions. */
@Configuration(proxyBeanMethods = false)
public class IdentityConfig {

    @Bean
    LoginThrottle loginThrottle(Clock clock) {
        return new InMemoryLoginThrottle(5, Duration.ofMinutes(15), clock);
    }

    @Bean
    SessionIssuer sessionIssuer(AccessTokenIssuer access, RefreshTokens refresh, Clock clock, StackcraftProperties props) {
        return new SessionIssuer(access, refresh, clock, props.auth().refreshTtl());
    }

    @Bean
    RegisterUseCase registerUseCase(UserAccounts accounts, PasswordHasher hasher, SessionIssuer sessions, Clock clock) {
        return new RegisterUseCase(accounts, hasher, sessions, clock);
    }

    @Bean
    LoginUseCase loginUseCase(UserAccounts accounts, PasswordHasher hasher, LoginThrottle throttle, SessionIssuer sessions) {
        return new LoginUseCase(accounts, hasher, throttle, sessions);
    }

    @Bean
    RefreshSessionUseCase refreshSessionUseCase(RefreshTokens tokens, UserAccounts accounts, SessionIssuer sessions, Clock clock) {
        return new RefreshSessionUseCase(tokens, accounts, sessions, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(RefreshTokens tokens, Clock clock) {
        return new LogoutUseCase(tokens, clock);
    }

    @Bean
    ProfileUseCase profileUseCase(UserAccounts accounts) {
        return new ProfileUseCase(accounts);
    }

    @Bean
    SessionCookies sessionCookies(StackcraftProperties props, Clock clock) {
        return new SessionCookies(props.auth().secureCookies(), clock);
    }
}
