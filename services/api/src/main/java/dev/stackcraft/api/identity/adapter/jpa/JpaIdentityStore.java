package dev.stackcraft.api.identity.adapter.jpa;

import dev.stackcraft.api.identity.domain.Email;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.identity.domain.RefreshToken;
import dev.stackcraft.api.identity.domain.UserAccount;
import dev.stackcraft.api.identity.port.RefreshTokens;
import dev.stackcraft.api.identity.port.UserAccounts;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adapter: accounts and refresh tokens in the relational DB (Flyway V2). */
public class JpaIdentityStore implements UserAccounts, RefreshTokens {

    interface Users extends JpaRepository<UserAccountEntity, UUID> {
        Optional<UserAccountEntity> findByEmail(String email);
    }

    interface Tokens extends JpaRepository<RefreshTokenEntity, UUID> {
        Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

        /** Compare-and-set in one statement: the database decides which concurrent refresh wins. */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("update RefreshTokenEntity t set t.replacedById = :next where t.id = :id and t.replacedById is null and t.revokedAt is null")
        int markRotated(@Param("id") UUID id, @Param("next") UUID next);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("update RefreshTokenEntity t set t.revokedAt = :at where t.familyId = :family and t.revokedAt is null")
        int revokeFamily(@Param("family") UUID family, @Param("at") Instant at);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("update RefreshTokenEntity t set t.revokedAt = :at where t.userId = :user and t.revokedAt is null")
        int revokeAllForUser(@Param("user") UUID user, @Param("at") Instant at);
    }

    private final Users users;
    private final Tokens tokens;

    JpaIdentityStore(Users users, Tokens tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> byEmail(Email email) {
        return users.findByEmail(email.value()).map(UserAccountEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> byId(UUID id) {
        return users.findById(id).map(UserAccountEntity::toDomain);
    }

    /** The unique index on email is the real guard against two simultaneous registrations. */
    @Override
    public UserAccount save(UserAccount account) {
        try {
            return users.saveAndFlush(UserAccountEntity.from(account)).toDomain();
        } catch (DataIntegrityViolationException e) {
            throw new IdentityException.EmailTaken();
        }
    }

    @Override
    @Transactional
    public void save(RefreshToken token) {
        tokens.save(RefreshTokenEntity.from(token));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> byHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean markRotated(UUID id, UUID replacedById) {
        return tokens.markRotated(id, replacedById) == 1;
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, Instant at) {
        tokens.revokeFamily(familyId, at);
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId, Instant at) {
        tokens.revokeAllForUser(userId, at);
    }
}
