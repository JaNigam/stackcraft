# 10. Identity and sessions

Decision record: [ADR 0004](../adr/0004-authentication-and-sessions.md). This chapter shows how it is built.

## The pieces

```
services/api/.../identity/
  domain/       Email, DisplayName, PasswordPolicy (value objects), UserAccount, RefreshToken, IdentityException (sealed)
  port/         UserAccounts, RefreshTokens, PasswordHasher, AccessTokenIssuer, LoginThrottle
  application/  RegisterUseCase, LoginUseCase, RefreshSessionUseCase, LogoutUseCase, ProfileUseCase, SessionIssuer
  adapter/      jpa/ (JpaIdentityStore), security/ (SecurityConfig, JwtAccessTokenIssuer, JwtKeys),
                web/ (AuthController, MeController, SessionCookies, IdentityErrorHandler), InMemoryLoginThrottle
progress/application/ImportGuestProgressUseCase + progress/adapter/web/ImportController
apps/web/src/features/auth/   AuthService port, RemoteAuthService, session.tsx, AuthPage, AccountMenu
apps/web/src/shared/api/http.ts   authFetch (cookies, CSRF header, single-flight refresh)
```

Only `identity.adapter.security` may import Spring Security or Nimbus (an ArchUnit rule). Progress code never
learned that accounts exist: `CurrentLearner.id()` now returns the JWT subject instead of `"local"`, and every use
case already took a learner id.

## Sign-in to answered question

```mermaid
sequenceDiagram
  participant B as Browser
  participant S as Security chain
  participant A as AuthController
  participant L as LoginUseCase
  participant Q as QuizController
  B->>S: GET /api/v1/series
  S-->>B: 200 + XSRF-TOKEN cookie (readable)
  B->>A: POST /auth/login {email, password} + X-XSRF-TOKEN
  A->>L: login(email, password, clientIp)
  L-->>A: Session(access JWT, refresh token)
  A-->>B: 200 Me + Set-Cookie sc_access (HttpOnly, Lax, /api) + sc_refresh (HttpOnly, Strict, /api/v1/auth)
  B->>S: POST /chapters/.../answers + sc_access cookie + X-XSRF-TOKEN
  S->>S: CookieJwtFilter: verify RS256, iss, exp; CsrfFilter: header == cookie
  S->>Q: authenticated as sub = user id
```

## Why a custom cookie filter instead of `oauth2ResourceServer()`

Spring's resource-server support exempts any request carrying a bearer token from CSRF. That is correct for an
`Authorization` header (a malicious site cannot set it), but our token rides in a cookie the browser attaches on its
own, so CSRF must still apply. `AuthFlowTest.writesWithoutTheCsrfHeaderAreRejected` caught this on the first run; the
fix is `SecurityConfig.CookieJwtFilter`, which decodes the cookie with the same `NimbusJwtDecoder` and leaves CSRF
fully on.

## Refresh, rotation, reuse

- `RefreshSessionUseCase` looks up the presented token by its SHA-256 hash.
- Already rotated or revoked → **reuse**: revoke the whole family (all tokens from that sign-in) and answer 401.
- Active → issue the next token in the family, then `markRotated(oldId, newId)`: a single
  `update ... where replaced_by_id is null and revoked_at is null`. If two requests race with the same token, the
  database lets exactly one win; the other is treated as reuse.
- The web never races itself: `authFetch` keeps one refresh in flight per tab, and `navigator.locks` serialises
  refreshes across tabs (they share cookies, so the second tab uses the already-rotated cookie).

## Guests and the first sign-in

- Guests keep using `LocalProgressService` (browser storage) even when the api is up.
- `SessionProvider` (web) is the single source of "who is this": `loading | guest | signedIn(me)`.
  `useProgressService()` returns the api adapter only when signed in. Progress query keys include the learner id,
  so cached XP can never show the previous person's numbers.
- After sign-in or sign-up, `exportGuest()` sends the browser's events (without XP numbers), open traps and Leitner
  states to `POST /api/v1/progress/imports`. The server re-awards each event from the content through
  `AwardXpUseCase` (same no-farming rule, attempt ids dedupe), then the browser copy is cleared (one backup kept).

## Abuse limits

| Threat | Defence |
|---|---|
| Password guessing | 5 failures per email+client per 15 min → 429 `Retry-After: 900` |
| Account enumeration on login | one message, dummy BCrypt check for unknown emails |
| Stolen refresh token | rotation + family revocation on reuse; only hashes stored |
| XSS reading tokens | both session cookies are HttpOnly |
| CSRF | SameSite plus the XSRF double-submit header on every write |
| Forged XP via import | XP re-derived from content; scores clamped; future and ancient timestamps skipped; 2,000-event cap |
| Leaked database | BCrypt (cost 12) passwords, SHA-256 refresh-token hashes, no access tokens stored |

## Tests that pin this down

`IdentityUseCasesTest` (rules with in-memory ports), `AuthFlowTest` (real chain: cookie flags, 401/403, isolation
between users, rotation and reuse, throttle, logout and logout-all, profile, import), `ImportGuestProgressUseCaseTest`,
and on the web `http.test.ts` (CSRF header, single refresh, retry with body, expiry signal) and `session.test.tsx`
(guest → signed-in switch, import once, restore on load).

## Interview angle

"Design login for a web app." Walk through access vs refresh tokens, where each lives and why (HttpOnly cookies, not
localStorage), CSRF with cookie auth, rotation with reuse detection, logout semantics with stateless tokens, and how
you would scale the throttle (shared store) and the key (rotation with `kid`).
