# ADR 0004: Authentication and sessions

- Status: Accepted (local-only for now; revisit cookie settings before public hosting)
- Date: 2026-11-19

## Context
v1 had one learner (`CurrentLearner.LOCAL`). Learners now need accounts so XP, streaks and the Trap Board are
personal, and so a leaderboard can compare people. Sign-in starts with email + password; GitHub and Google follow on
the same session design. Guests must keep working, and their browser progress must survive the first sign-in.

## Decision
**Short-lived access token + rotating refresh token, both in HttpOnly cookies, issued by the api itself.**

| | Access token | Refresh token |
|---|---|---|
| Format | JWT, RS256, claims `sub` (user id), `name`, `iss=stackcraft-api` | 256-bit random, opaque |
| Lifetime | 10 minutes | 30 days |
| Cookie | `sc_access`, HttpOnly, SameSite=Lax, Path=/api | `sc_refresh`, HttpOnly, SameSite=Strict, Path=/api/v1/auth |
| Stored server-side | no (stateless check of the signature) | only its SHA-256 hash, in `refresh_token` |

- **Rotation:** every `/auth/refresh` marks the presented token replaced and issues a new one in the same family.
- **Reuse detection:** presenting a token that was already replaced or revoked revokes the whole family: either the
  user or a thief is replaying an old token, and both must sign in again.
- **Logout** revokes the family and clears the cookies; **log out everywhere** revokes every family of the user.
  An access token already issued stays valid until it expires (at most 10 minutes). Accepted trade-off for
  stateless checks; a deny-list can be added if that window ever matters.
- **CSRF:** cookies are sent automatically, so writes need a CSRF token: Spring Security's SPA mode (readable
  `XSRF-TOKEN` cookie, the web echoes it in `X-XSRF-TOKEN`) on top of SameSite.
- **Passwords:** BCrypt (cost 12) via Spring's `DelegatingPasswordEncoder`; 10+ characters and not the email.
- **Brute force:** at most 5 failed logins per email and client per 15 minutes (429), behind a `LoginThrottle` port
  so a shared (Redis) adapter can replace the in-memory one when there is more than one api instance.
- **No account enumeration on login:** one generic error, and unknown emails still run a dummy hash so timing does
  not leak. (Registration does say "email already registered": unavoidable without email verification.)
- **Keys:** RSA private key from `STACKCRAFT_JWT_PRIVATE_KEY` (PEM) in `prod`; generated at start-up in `dev` (every
  restart signs everyone out, which is fine locally). No key material in the repo.
- **Guest import:** on first sign-in the browser sends its guest XP events once. The server re-awards them through
  `AwardXpUseCase` (XP comes from the content, not the client; attempt ids dedupe; no-farming still applies).
  Honest limit: guest scores were graded in the browser, so the server trusts the *score* of imported events. The
  damage is bounded by the content (you can never import more XP than the chapters hold).

## Why not server-side sessions
A session store would make logout instant, but the api would stop being stateless (ADR 0001) and every instance
would need the store. JWT + refresh rotation keeps request handling stateless and only touches the database on
login, refresh and logout. It is also the design interviewers ask about (access vs refresh, rotation, revocation).

## Before public hosting
If the web (GitHub Pages) and the api end up on different sites, the cookies need `SameSite=None; Secure` plus CORS
with credentials, or (better) serve the api under the same domain. Add email verification and password reset at the
same time.
