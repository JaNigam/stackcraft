package dev.stackcraft.api.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Value object: trimmed, lower-cased, sanity-checked. Lower-casing here makes the unique index case-insensitive. */
public record Email(String value) {
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");

    public Email {
        if (value == null) throw new IdentityException.InvalidInput("Email is required");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > 254 || !SHAPE.matcher(value).matches()) {
            throw new IdentityException.InvalidInput("That does not look like an email address");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
