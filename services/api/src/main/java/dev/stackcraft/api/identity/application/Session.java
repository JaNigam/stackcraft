package dev.stackcraft.api.identity.application;

import dev.stackcraft.api.identity.domain.UserAccount;

import java.time.Instant;

/** What a successful sign-in or refresh hands to the web adapter, which turns it into two cookies. */
public record Session(UserAccount user, String accessToken, Instant accessExpiresAt, String refreshToken,
                      Instant refreshExpiresAt) {
}
