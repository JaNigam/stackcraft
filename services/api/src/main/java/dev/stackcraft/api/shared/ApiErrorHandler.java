package dev.stackcraft.api.shared;

import dev.stackcraft.api.execution.port.CodeExecutor.ExecutorBusyException;
import dev.stackcraft.api.execution.port.CodeExecutor.ExecutorUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/** Every error is an RFC 9457 problem document with a stable {@code type} the UI can switch on. */
@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {

    private static final String TYPES = "https://stackcraft.dev/problems/";

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Not found", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "bad-request", "Bad request", e.getMessage());
    }

    @ExceptionHandler(ExecutorBusyException.class)
    ResponseEntity<ProblemDetail> busy(ExecutorBusyException e) {
        ResponseEntity<ProblemDetail> r = problem(HttpStatus.TOO_MANY_REQUESTS, "runner-busy", "Runner busy", e.getMessage());
        return ResponseEntity.status(r.getStatusCode()).header(HttpHeaders.RETRY_AFTER, "2").body(r.getBody());
    }

    @ExceptionHandler(ExecutorUnavailableException.class)
    ResponseEntity<ProblemDetail> unavailable(ExecutorUnavailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "runner-unavailable", "Runner unavailable", e.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPES + type));
        pd.setTitle(title);
        return ResponseEntity.status(status).body(pd);
    }
}
