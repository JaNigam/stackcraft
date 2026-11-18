package dev.stackcraft.api.identity.adapter.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.port.AccessTokenIssuer;
import dev.stackcraft.api.identity.port.PasswordHasher;
import dev.stackcraft.api.shared.CurrentLearner;
import dev.stackcraft.api.shared.StackcraftProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

/**
 * The security edge of the api (ADR 0004). Stateless: no HTTP session; who you are comes from the {@code sc_access}
 * JWT cookie on every request. CSRF uses Spring Security's SPA mode (XSRF-TOKEN cookie echoed in X-XSRF-TOKEN).
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    static final String ACCESS_COOKIE = "sc_access";

    /** Public endpoints never look at the access cookie, so a stale cookie cannot break them. */
    static final RequestMatcher PUBLIC = request -> anyMatches(request,
            pathPattern(HttpMethod.GET, "/api/v1/series"),
            pathPattern(HttpMethod.GET, "/api/v1/series/*"),
            pathPattern(HttpMethod.GET, "/api/v1/chapters/*"),
            pathPattern("/api/v1/runs"),
            pathPattern("/api/v1/runs/**"),
            pathPattern(HttpMethod.POST, "/api/v1/auth/register"),
            pathPattern(HttpMethod.POST, "/api/v1/auth/login"),
            pathPattern(HttpMethod.POST, "/api/v1/auth/refresh"),
            pathPattern(HttpMethod.POST, "/api/v1/auth/logout"),
            pathPattern("/actuator/health"),
            pathPattern("/actuator/health/**"),
            pathPattern("/actuator/info"),
            pathPattern("/error"));

    private static boolean anyMatches(HttpServletRequest request, RequestMatcher... matchers) {
        for (RequestMatcher m : matchers) if (m.matches(request)) return true;
        return false;
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder decoder) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.spa())
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .authorizeHttpRequests(a -> a.requestMatchers(PUBLIC).permitAll().anyRequest().authenticated())
                // Not oauth2ResourceServer(): it exempts bearer-token requests from CSRF, which is right for an
                // Authorization header but wrong for a cookie the browser attaches by itself (AuthFlowTest caught it).
                .addFilterBefore(new CookieJwtFilter(decoder), AnonymousAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> Problems.write(res, 401, "unauthenticated", "Unauthenticated", "Sign in to do that"))
                        .accessDeniedHandler((req, res, ex) -> Problems.write(res, 403, "forbidden", "Forbidden",
                                "Missing or invalid X-XSRF-TOKEN header")))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable());
        return http.build();
    }

    /**
     * Authenticates from the JWT in the HttpOnly {@code sc_access} cookie (never from JS-visible storage). Public
     * endpoints skip it, so a stale cookie cannot break them; an invalid token simply leaves the request anonymous
     * and the entry point answers 401.
     */
    static final class CookieJwtFilter extends OncePerRequestFilter {
        private final JwtDecoder decoder;

        CookieJwtFilter(JwtDecoder decoder) {
            this.decoder = decoder;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String token = PUBLIC.matches(req) ? null : accessCookie(req);
            if (token != null) {
                try {
                    Jwt jwt = decoder.decode(token);
                    SecurityContext ctx = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
                    ctx.setAuthentication(new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject()));
                    SecurityContextHolder.getContextHolderStrategy().setContext(ctx);
                } catch (JwtException expiredOrForged) {
                    // stays anonymous
                }
            }
            chain.doFilter(req, res);
        }

        private static String accessCookie(HttpServletRequest req) {
            if (req.getCookies() == null) return null;
            for (Cookie c : req.getCookies()) {
                if (ACCESS_COOKIE.equals(c.getName()) && !c.getValue().isBlank()) return c.getValue();
            }
            return null;
        }
    }

    @Bean
    JwtKeys jwtKeys(StackcraftProperties props) {
        return JwtKeys.from(props.auth().jwtPrivateKey());
    }

    @Bean
    JwtEncoder jwtEncoder(JwtKeys keys) {
        RSAKey jwk = new RSAKey.Builder(keys.publicKey()).privateKey(keys.privateKey()).keyID("stackcraft-1").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtKeys keys) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ofSeconds(30)),
                new JwtIssuerValidator(JwtAccessTokenIssuer.ISSUER)));
        return decoder;
    }

    @Bean
    AccessTokenIssuer accessTokenIssuer(JwtEncoder encoder, StackcraftProperties props, Clock clock) {
        return new JwtAccessTokenIssuer(encoder, props.auth().accessTtl(), clock);
    }

    /** BCrypt cost 12 behind a delegating encoder, so the algorithm can be upgraded later ({bcrypt} prefix). */
    @Bean
    PasswordHasher passwordHasher() {
        PasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));
        return new PasswordHasher() {
            @Override
            public String hash(String raw) {
                return encoder.encode(raw);
            }

            @Override
            public boolean matches(String raw, String hash) {
                return encoder.matches(raw, hash);
            }
        };
    }

    /** Who is calling, from the validated JWT. Use cases still just receive a learnerId string. */
    @Bean
    CurrentLearner currentLearner() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                throw new IdentityException.Unauthenticated();
            }
            return auth.getName();
        };
    }

    /** Touching the deferred token makes Spring write the XSRF-TOKEN cookie on every response, GETs included. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
            if (token != null) token.getToken();
            chain.doFilter(req, res);
        }
    }

    static final class Problems {
        private Problems() {}

        static void write(HttpServletResponse res, int status, String type, String title, String detail) throws IOException {
            res.setStatus(status);
            res.setContentType("application/problem+json");
            res.getWriter().write("{\"type\":\"https://stackcraft.dev/problems/" + type + "\",\"title\":\"" + title
                    + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
        }
    }
}
