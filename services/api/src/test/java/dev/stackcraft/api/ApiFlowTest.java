package dev.stackcraft.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * The whole api with real adapters: fixture content from files, Flyway-migrated H2, and a runner URL that points
 * nowhere (so we also see the 503 path). Mirrors what the browser does.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApiFlowTest {

    private static final String CH = "/api/v1/chapters/demo.u00.l01";

    @Autowired
    MockMvcTester mvc;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    /** Every test runs as a freshly registered learner, the way the browser does: access cookie + CSRF token. */
    private Cookie access;

    @BeforeEach
    void signUp() {
        var res = mvc.post().uri("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"flow-" + id() + "@example.com\",\"password\":\"correct horse battery\",\"displayName\":\"Flow Tester\"}")
                .exchange();
        assertThat(res).hasStatus(201);
        access = res.getResponse().getCookie("sc_access");
        assertThat(access).isNotNull();
    }

    private MockMvcTester.MockMvcRequestBuilder post(String uri, String json) {
        return mvc.post().uri(uri).cookie(access).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private MockMvcTester.MockMvcRequestBuilder get(String uri) {
        return mvc.get().uri(uri).cookie(access);
    }

    @Test
    void servesSeriesAndChapterWithTypedQuestions() {
        assertThat(mvc.get().uri("/api/v1/series")).hasStatusOk().bodyJson().extractingPath("$[0].chapterCount").isEqualTo(1);
        assertThat(mvc.get().uri("/api/v1/series/demo")).hasStatusOk().bodyJson()
                .extractingPath("$.units[0].chapters[0].title").isEqualTo("Basics");
        var chapter = assertThat(mvc.get().uri(CH)).hasStatusOk().bodyJson();
        chapter.extractingPath("$.questions[*].type").asArray().containsExactly("mcq", "predict", "short", "problem");
        chapter.extractingPath("$.meta.whereAsked[0]").isEqualTo("Fixture interview");
        chapter.extractingPath("$.flashcardCount").isEqualTo(1);
    }

    @Test
    void unknownChapterIsAProblem404() {
        assertThat(mvc.get().uri("/api/v1/chapters/nope")).hasStatus(404)
                .bodyJson().extractingPath("$.type").isEqualTo("https://stackcraft.dev/problems/not-found");
    }

    @Test
    void retryWithSameAttemptIdNeverDoubleAwards() {
        String attempt = id();
        String body = "{\"attemptId\":\"" + attempt + "\",\"choice\":1}";

        var first = assertThat(post(CH + "/questions/w1/answers", body)).hasStatusOk().bodyJson();
        first.extractingPath("$.correct").isEqualTo(true);
        first.extractingPath("$.xpAwarded").isEqualTo(2);
        first.extractingPath("$.progress.totalXp").isEqualTo(2);

        var retry = assertThat(post(CH + "/questions/w1/answers", body)).hasStatusOk().bodyJson();
        retry.extractingPath("$.duplicate").isEqualTo(true);
        retry.extractingPath("$.progress.totalXp").isEqualTo(2);

        var newAttemptSameQuestion = assertThat(post(CH + "/questions/w1/answers", "{\"attemptId\":\"" + id() + "\",\"choice\":1}"))
                .hasStatusOk().bodyJson();
        newAttemptSameQuestion.extractingPath("$.xpAwarded").isEqualTo(0);
        newAttemptSameQuestion.extractingPath("$.progress.totalXp").isEqualTo(2);
    }

    @Test
    void wrongAnswersLandOnTheTrapBoardAndRightOnesClearIt() {
        assertThat(post(CH + "/questions/i1/answers", "{\"attemptId\":\"" + id() + "\",\"text\":\"6\"}"))
                .hasStatusOk().bodyJson().extractingPath("$.correct").isEqualTo(false);
        assertThat(post(CH + "/teasers/t1/commits", "{\"attemptId\":\"" + id() + "\",\"choice\":0}"))
                .hasStatusOk().bodyJson().extractingPath("$.reveal").asString().contains("HashMap");

        assertThat(get("/api/v1/trap-board")).hasStatusOk().bodyJson()
                .extractingPath("$[*].itemId").asArray().containsExactlyInAnyOrder("i1", "t1");

        assertThat(post(CH + "/questions/i1/answers", "{\"attemptId\":\"" + id() + "\",\"text\":\"33\\n\"}"))
                .hasStatusOk().bodyJson().extractingPath("$.xpAwarded").isEqualTo(4);
        assertThat(get("/api/v1/trap-board")).hasStatusOk().bodyJson()
                .extractingPath("$[*].itemId").asArray().containsExactly("t1");
    }

    @Test
    void completingAChapterUnlocksItsFlashcards() {
        assertThat(get("/api/v1/flashcards/due")).hasStatusOk().bodyJson().extractingPath("$").asArray().isEmpty();

        assertThat(post(CH + "/completion", "{\"attemptId\":\"" + id() + "\"}")).hasStatusOk().bodyJson()
                .extractingPath("$.progress.completedChapters[0]").isEqualTo("demo.u00.l01");

        assertThat(get("/api/v1/flashcards/due")).hasStatusOk().bodyJson()
                .extractingPath("$[0].key").isEqualTo("demo.u00.l01/f1");

        String review = "{\"attemptId\":\"" + id() + "\",\"cardKey\":\"demo.u00.l01/f1\",\"remembered\":true}";
        assertThat(post("/api/v1/flashcards/reviews", review)).hasStatusOk().bodyJson().extractingPath("$.box").isEqualTo(2);
        assertThat(get("/api/v1/flashcards/due")).hasStatusOk().bodyJson().extractingPath("$").asArray().isEmpty();
    }

    @Test
    void selfScoredProblemWithHintsEarnsPartialXp() {
        assertThat(post(CH + "/questions/k1/answers", "{\"attemptId\":\"" + id() + "\",\"selfScore\":2,\"hintsUsed\":1}"))
                .hasStatusOk().bodyJson().extractingPath("$.xpAwarded").isEqualTo(6); // round(8 * 0.8)
    }

    @Test
    void badInputIs400() {
        assertThat(post(CH + "/questions/w1/answers", "{\"choice\":1}")).hasStatus(400);
        assertThat(post(CH + "/questions/w1/answers", "{\"attemptId\":\"x\",\"choice\":1}")).hasStatus(400);
        assertThat(post(CH + "/teasers/t1/commits", "{\"attemptId\":\"" + id() + "\",\"choice\":9}")).hasStatus(400);
    }

    @Test
    void runnerDownIs503WithAStableProblemType() {
        assertThat(post("/api/v1/runs", "{\"source\":\"public class A { public static void main(String[] a) {} }\"}"))
                .hasStatus(503).bodyJson().extractingPath("$.type").isEqualTo("https://stackcraft.dev/problems/runner-unavailable");
    }

    @Test
    void corsAllowsOnlyTheWebOrigin() {
        assertThat(mvc.options().uri("/api/v1/progress").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")).hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        assertThat(mvc.options().uri("/api/v1/progress").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET")).hasStatus(403);
    }

    @Test
    void healthProbesAreUp() {
        assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk();
        assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
    }
}
