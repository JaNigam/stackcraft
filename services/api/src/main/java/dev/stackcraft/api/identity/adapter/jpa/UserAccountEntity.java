package dev.stackcraft.api.identity.adapter.jpa;

import dev.stackcraft.api.identity.domain.DisplayName;
import dev.stackcraft.api.identity.domain.Email;
import dev.stackcraft.api.identity.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_account")
class UserAccountEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 254, unique = true)
    String email;

    @Column(name = "display_name", nullable = false, length = 32)
    String displayName;

    @Column(name = "password_hash", length = 100)
    String passwordHash;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "leaderboard_visible", nullable = false)
    boolean leaderboardVisible;

    protected UserAccountEntity() {}

    static UserAccountEntity from(UserAccount u) {
        UserAccountEntity e = new UserAccountEntity();
        e.id = u.id();
        e.email = u.email().value();
        e.displayName = u.displayName().value();
        e.passwordHash = u.passwordHash();
        e.createdAt = u.createdAt();
        e.leaderboardVisible = u.leaderboardVisible();
        return e;
    }

    UserAccount toDomain() {
        return new UserAccount(id, new Email(email), new DisplayName(displayName), passwordHash, createdAt, leaderboardVisible);
    }
}
