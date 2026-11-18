package dev.stackcraft.api.identity.domain;

import java.util.regex.Pattern;

/** What other learners see on the leaderboard. Never the email. */
public record DisplayName(String value) {
    private static final Pattern ALLOWED = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} ._-]{0,30}[\\p{L}\\p{N}]$");

    public DisplayName {
        if (value == null) throw new IdentityException.InvalidInput("Display name is required");
        value = value.strip().replaceAll("\\s+", " ");
        if (!ALLOWED.matcher(value).matches()) {
            throw new IdentityException.InvalidInput("Display name: 2-32 letters, digits, spaces, '.', '_' or '-'");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
