package dev.stackcraft.api.shared;

/**
 * Port: who is calling. Implemented from the validated access token (identity.adapter.security, ADR 0004); use cases
 * only ever see the learner id string, so they did not change when accounts arrived.
 */
@FunctionalInterface
public interface CurrentLearner {

    /** @throws dev.stackcraft.api.identity.domain.IdentityException.Unauthenticated when nobody is signed in */
    String id();
}
