package dev.stackcraft.api.shared;

/** Mapped to 404 by {@code ApiErrorHandler}. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, String id) {
        super(what + " '" + id + "' not found");
    }
}
