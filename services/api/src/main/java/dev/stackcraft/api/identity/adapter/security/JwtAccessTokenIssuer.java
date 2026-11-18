package dev.stackcraft.api.identity.adapter.security;

import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.AccessTokenIssuer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Adapter: RS256 JWT with sub = user id, name = display name, 10-minute lifetime. */
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ISSUER = "stackcraft-api";

    private final JwtEncoder encoder;
    private final Duration ttl;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder encoder, Duration ttl, Clock clock) {
        this.encoder = encoder;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(UserAccount user) {
        Instant now = clock.instant();
        Instant exp = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.id().toString())
                .claim("name", user.displayName().value())
                .issuedAt(now)
                .expiresAt(exp)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), exp);
    }
}
