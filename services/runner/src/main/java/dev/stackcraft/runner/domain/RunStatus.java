package dev.stackcraft.runner.domain;

/** Outcome category. The UI picks its message from this, never by parsing stderr. */
public enum RunStatus {
    OK,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIMEOUT,
    OUTPUT_LIMIT
}
