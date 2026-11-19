package dev.stackcraft.api.identity.adapter.web;

import dev.stackcraft.api.identity.application.Session;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Clock;
import java.time.Duration;

/**
 * Writes the two session cookies (ADR 0004). Both HttpOnly, so page JavaScript (and any XSS) cannot read them.
 * The refresh cookie is only ever sent to /api/v1/auth, which keeps it off every other request.
 */
public class SessionCookies {

    public static final String ACCESS = "sc_access";
    public static final String REFRESH = "sc_refresh";
    private static final String ACCESS_PATH = "/api";
    private static final String REFRESH_PATH = "/api/v1/auth";

    private final boolean secure;
    private final Clock clock;

    public SessionCookies(boolean secure, Clock clock) {
        this.secure = secure;
        this.clock = clock;
    }

    public void write(HttpServletResponse res, Session s) {
        add(res, ACCESS, s.accessToken(), ACCESS_PATH, "Lax", Duration.between(clock.instant(), s.accessExpiresAt()));
        add(res, REFRESH, s.refreshToken(), REFRESH_PATH, "Strict", Duration.between(clock.instant(), s.refreshExpiresAt()));
    }

    public void clear(HttpServletResponse res) {
        add(res, ACCESS, "", ACCESS_PATH, "Lax", Duration.ZERO);
        add(res, REFRESH, "", REFRESH_PATH, "Strict", Duration.ZERO);
    }

    private void add(HttpServletResponse res, String name, String value, String path, String sameSite, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite(sameSite)
                .path(path).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
