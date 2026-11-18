package dev.stackcraft.api.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Refresh tokens: 256 random bits, URL-safe; stored only as SHA-256 hex. */
public final class OpaqueTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OpaqueTokens() {}

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** No salt needed: the input is already 256 bits of randomness, so it cannot be brute-forced or looked up. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 missing from the JDK", e);
        }
    }
}
