package dev.stackcraft.api.identity.domain;

/**
 * Length beats complexity rules (NIST SP 800-63B): at least 10 characters, at most 128, and not the email itself.
 * No "must contain a symbol" rules: they push people to predictable patterns.
 */
public final class PasswordPolicy {
    public static final int MIN = 10;
    public static final int MAX = 128;

    private PasswordPolicy() {}

    public static void check(String password, Email email) {
        if (password == null || password.length() < MIN) {
            throw new IdentityException.InvalidInput("Password must be at least " + MIN + " characters");
        }
        if (password.length() > MAX) throw new IdentityException.InvalidInput("Password must be at most " + MAX + " characters");
        if (password.strip().equalsIgnoreCase(email.value()) || password.strip().equalsIgnoreCase(email.value().split("@")[0])) {
            throw new IdentityException.InvalidInput("Password must not be your email");
        }
    }
}
