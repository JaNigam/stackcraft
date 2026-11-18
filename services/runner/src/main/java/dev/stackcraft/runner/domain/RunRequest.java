package dev.stackcraft.runner.domain;

import java.util.Objects;

/**
 * What the caller wants to run: one Java source file plus optional stdin.
 * A value object (record): immutable, compared by value, validated on construction.
 */
public record RunRequest(String source, String stdin) {
    public RunRequest {
        Objects.requireNonNull(source, "source");
        if (source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        stdin = stdin == null ? "" : stdin;
    }
}
