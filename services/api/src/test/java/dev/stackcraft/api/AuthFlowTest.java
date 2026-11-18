package dev.stackcraft.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Real security chain, real JWTs, real cookies: what a browser sees. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthFlowTest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    MockMvcTester mvc;

    private static String email() {
        return "u-" + UUID.randomUUID() + "@example.com";
    }

    private MvcTestResult register(String email, String name) {
        return mvc.post().uri("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\"" + name + "\"}").exchange();
    }

    private static Cookie cookie(MvcTestResult r, String name) {
        return r.getResponse().getCookie(name);
    }

    private MvcTestResult answerW1(Cookie access) {
        return mvc.post().uri("/api/v1/chapters/demo.u00.l01/questions/w1/answers").cookie(access).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"attemptId\":\"" + UUID.randomUUID() + "\",\"choice\":1}").exchange();
    }

    @Test
    void registerSetsHttpOnlySessionCookiesAndMeWorks() throws Exception {
        MvcTestResult r = register(email(), "Ana");
        assertThat(r).hasStatus(201).bodyJson().extractingPath("$.displayName").isEqualTo("Ana");
        assertThat(r.getResponse().getHeaders("Set-Cookie")).anySatisfy(h -> assertThat(h).startsWith("sc_access=").contains("HttpOnly", "SameSite=Lax", "Path=/api"))
                .anySatisfy(h -> assertThat(h).startsWith("sc_refresh=").contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth"));
        assertThat(r.getResponse().getContentAsString()).doesNotContain("password");

        assertThat(mvc.get().uri("/api/v1/me").cookie(cookie(r, "sc_access"))).hasStatusOk().bodyJson().extractingPath("$.leaderboardVisible").isEqualTo(true);
    }

    @Test
    void protectedEndpointsNeedAnAccessTokenButContentIsPublic() {
        assertThat(mvc.get().uri("/api/v1/progress")).hasStatus(401).bodyJson().extractingPath("$.type").isEqualTo("https://stackcraft.dev/problems/unauthenticated");
        assertThat(mvc.get().uri("/api/v1/series")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/series").cookie(new Cookie("sc_access", "garbage"))).hasStatusOk(); // stale cookie never breaks public pages
        assertThat(mvc.get().uri("/api/v1/progress").cookie(new Cookie("sc_access", "garbage"))).hasStatus(401);
    }

    @Test
    void writesWithoutTheCsrfHeaderAreRejected() {
        Cookie access = cookie(register(email(), "Ana"), "sc_access");
        assertThat(mvc.post().uri("/api/v1/chapters/demo.u00.l01/questions/w1/answers").cookie(access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"attemptId\":\"" + UUID.randomUUID() + "\",\"choice\":1}"))
                .hasStatus(403);
    }

    @Test
    void eachLearnerHasTheirOwnProgress() {
        Cookie ana = cookie(register(email(), "Ana"), "sc_access");
        Cookie bo = cookie(register(email(), "Bo"), "sc_access");

        assertThat(answerW1(ana)).hasStatusOk().bodyJson().extractingPath("$.progress.totalXp").isEqualTo(2);
        assertThat(mvc.get().uri("/api/v1/progress").cookie(bo)).hasStatusOk().bodyJson().extractingPath("$.totalXp").isEqualTo(0);
        assertThat(mvc.get().uri("/api/v1/progress").cookie(ana)).hasStatusOk().bodyJson().extractingPath("$.totalXp").isEqualTo(2);
    }

    @Test
    void loginRefreshRotationAndReuseDetection() {
        String e = email();
        register(e, "Ana");
        MvcTestResult login = mvc.post().uri("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e.toUpperCase() + "\",\"password\":\"" + PASSWORD + "\"}").exchange();
        assertThat(login).hasStatusOk();
        Cookie firstRefresh = cookie(login, "sc_refresh");

        MvcTestResult rotated = mvc.post().uri("/api/v1/auth/refresh").cookie(firstRefresh).with(csrf()).exchange();
        assertThat(rotated).hasStatusOk();
        Cookie secondRefresh = cookie(rotated, "sc_refresh");
        assertThat(secondRefresh.getValue()).isNotEqualTo(firstRefresh.getValue());
        assertThat(answerW1(cookie(rotated, "sc_access"))).hasStatusOk();

        // Replaying the first refresh token is treated as theft: the family is revoked and cookies cleared.
        MvcTestResult replay = mvc.post().uri("/api/v1/auth/refresh").cookie(firstRefresh).with(csrf()).exchange();
        assertThat(replay).hasStatus(401).bodyJson().extractingPath("$.type").isEqualTo("https://stackcraft.dev/problems/session-ended");
        assertThat(cookie(replay, "sc_refresh").getMaxAge()).isZero();
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(secondRefresh).with(csrf())).hasStatus(401);
    }

    @Test
    void wrongPasswordIsGenericAndRepeatedFailuresAreThrottled() {
        String e = email();
        register(e, "Ana");
        String wrong = "{\"email\":\"" + e + "\",\"password\":\"not the password\"}";
        for (int i = 0; i < 5; i++) {
            assertThat(mvc.post().uri("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .hasStatus(401).bodyJson().extractingPath("$.detail").isEqualTo("Email or password is wrong");
        }
        assertThat(mvc.post().uri("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e + "\",\"password\":\"" + PASSWORD + "\"}"))
                .hasStatus(429).hasHeader("Retry-After", "900");
    }

    @Test
    void logoutEndsTheSessionAndLogoutAllEndsEveryDevice() {
        String e = email();
        MvcTestResult laptop = register(e, "Ana");
        MvcTestResult phone = mvc.post().uri("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e + "\",\"password\":\"" + PASSWORD + "\"}").exchange();

        MvcTestResult out = mvc.post().uri("/api/v1/auth/logout").cookie(cookie(laptop, "sc_refresh")).with(csrf()).exchange();
        assertThat(out).hasStatus(204);
        assertThat(cookie(out, "sc_access").getMaxAge()).isZero();
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(cookie(laptop, "sc_refresh")).with(csrf())).hasStatus(401);

        assertThat(mvc.post().uri("/api/v1/auth/logout-all").cookie(cookie(phone, "sc_access")).with(csrf())).hasStatus(204);
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(cookie(phone, "sc_refresh")).with(csrf())).hasStatus(401);
    }

    @Test
    void duplicateEmailAndBadInputAreProblems() {
        String e = email();
        register(e, "Ana");
        assertThat(register(e, "Ana Again")).hasStatus(409).bodyJson().extractingPath("$.type").isEqualTo("https://stackcraft.dev/problems/email-taken");
        assertThat(register(email(), "x")).hasStatus(400);
    }

    @Test
    void profileUpdateAndGuestImport() {
        Cookie access = cookie(register(email(), "Ana"), "sc_access");
        assertThat(mvc.patch().uri("/api/v1/me").cookie(access).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Ana B\",\"leaderboardVisible\":false}"))
                .hasStatusOk().bodyJson().extractingPath("$.leaderboardVisible").isEqualTo(false);

        String body = "{\"events\":[{\"attemptId\":\"guest-attempt-01\",\"kind\":\"ANSWER\",\"chapterId\":\"demo.u00.l01\",\"itemId\":\"w1\",\"score\":1,\"at\":\"2026-11-18T09:00:00Z\"}]}";
        var first = assertThat(mvc.post().uri("/api/v1/progress/imports").cookie(access).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatusOk().bodyJson();
        first.extractingPath("$.xpAwarded").isEqualTo(2);
        first.extractingPath("$.progress.totalXp").isEqualTo(2);
        assertThat(mvc.post().uri("/api/v1/progress/imports").cookie(access).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatusOk().bodyJson().extractingPath("$.xpAwarded").isEqualTo(0);
    }
}
