package dev.stackcraft.api.identity.port;

/** Port so the use cases never depend on Spring Security's crypto classes. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
