package dev.stackcraft.api.identity.domain;

/** Every identity failure the web adapter maps to a problem response. Sealed: the mapping switch is exhaustive. */
public sealed abstract class IdentityException extends RuntimeException
        permits IdentityException.InvalidInput, IdentityException.EmailTaken, IdentityException.InvalidCredentials,
        IdentityException.TooManyAttempts, IdentityException.InvalidSession, IdentityException.Unauthenticated {

    protected IdentityException(String message) {
        super(message);
    }

    /** 400 */
    public static final class InvalidInput extends IdentityException {
        public InvalidInput(String message) { super(message); }
    }

    /** 409 */
    public static final class EmailTaken extends IdentityException {
        public EmailTaken() { super("That email is already registered. Sign in instead."); }
    }

    /** 401, deliberately vague: never says which of email or password was wrong. */
    public static final class InvalidCredentials extends IdentityException {
        public InvalidCredentials() { super("Email or password is wrong"); }
    }

    /** 429 */
    public static final class TooManyAttempts extends IdentityException {
        public TooManyAttempts() { super("Too many failed sign-ins. Wait 15 minutes and try again."); }
    }

    /** 401: refresh token missing, expired, revoked or reused. */
    public static final class InvalidSession extends IdentityException {
        public InvalidSession() { super("Your session has ended. Sign in again."); }
    }

    /** 401: no signed-in user for an endpoint that needs one. */
    public static final class Unauthenticated extends IdentityException {
        public Unauthenticated() { super("Sign in to do that"); }
    }
}
