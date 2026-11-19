package dev.stackcraft.api.identity.port;

/**
 * Port: brute-force protection. In-memory today (one api instance); a Redis adapter replaces it when the api scales
 * out, because a per-instance counter would let an attacker spread guesses across instances.
 */
public interface LoginThrottle {

    /** False once the key has too many recent failures. */
    boolean allowed(String key);

    void recordFailure(String key);

    void reset(String key);
}
