package dev.stackcraft.api.identity.adapter.web;

import dev.stackcraft.api.identity.domain.IdentityException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/** Identity failures as RFC 9457 problems. The sealed hierarchy makes this switch exhaustive. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityErrorHandler {

    @ExceptionHandler(IdentityException.class)
    ResponseEntity<ProblemDetail> handle(IdentityException e) {
        record Mapping(HttpStatus status, String type, String title) {}
        Mapping m = switch (e) {
            case IdentityException.InvalidInput x -> new Mapping(HttpStatus.BAD_REQUEST, "bad-request", "Bad request");
            case IdentityException.EmailTaken x -> new Mapping(HttpStatus.CONFLICT, "email-taken", "Email already registered");
            case IdentityException.InvalidCredentials x -> new Mapping(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Sign-in failed");
            case IdentityException.TooManyAttempts x -> new Mapping(HttpStatus.TOO_MANY_REQUESTS, "too-many-attempts", "Too many attempts");
            case IdentityException.InvalidSession x -> new Mapping(HttpStatus.UNAUTHORIZED, "session-ended", "Session ended");
            case IdentityException.Unauthenticated x -> new Mapping(HttpStatus.UNAUTHORIZED, "unauthenticated", "Unauthenticated");
        };
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(m.status(), e.getMessage());
        pd.setType(URI.create("https://stackcraft.dev/problems/" + m.type()));
        pd.setTitle(m.title());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(m.status());
        if (e instanceof IdentityException.TooManyAttempts) builder.header(HttpHeaders.RETRY_AFTER, "900");
        return builder.body(pd);
    }
}
