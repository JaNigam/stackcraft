-- Accounts and refresh tokens (ADR 0004). Runs unchanged on Postgres and on H2 (MODE=PostgreSQL).

create table user_account (
    id                  uuid primary key,
    email               varchar(254) not null,         -- stored lower-cased by the domain
    display_name        varchar(32)  not null,
    password_hash       varchar(100),                  -- null for future OAuth-only accounts
    created_at          timestamp(6) with time zone not null,
    leaderboard_visible boolean      not null default true,
    constraint uq_user_email unique (email)
);

create table refresh_token (
    id             uuid primary key,
    user_id        uuid         not null references user_account (id) on delete cascade,
    family_id      uuid         not null,              -- one family per sign-in; rotation stays in the family
    token_hash     varchar(64)  not null,              -- SHA-256 hex of the opaque token; the token itself is never stored
    created_at     timestamp(6) with time zone not null,
    expires_at     timestamp(6) with time zone not null,
    revoked_at     timestamp(6) with time zone,
    replaced_by_id uuid,
    constraint uq_refresh_hash unique (token_hash)
);
create index ix_refresh_user on refresh_token (user_id);
create index ix_refresh_family on refresh_token (family_id);

-- For the weekly leaderboard: sum(xp) of events since Monday, grouped by learner.
create index ix_xp_event_time on xp_event (occurred_at, learner_id);
