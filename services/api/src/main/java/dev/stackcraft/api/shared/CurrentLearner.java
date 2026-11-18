package dev.stackcraft.api.shared;

/**
 * Port: who is calling. v1 is single-learner ({@link #LOCAL}); the auth chapter swaps in a JWT-backed implementation
 * and no controller or use case changes.
 */
@FunctionalInterface
public interface CurrentLearner {

    CurrentLearner LOCAL = () -> "local";

    String id();
}
