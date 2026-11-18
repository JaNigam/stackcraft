package dev.stackcraft.runner.adapter.web;

import dev.stackcraft.runner.application.RunQueueFullException;
import dev.stackcraft.runner.application.SourceTooLargeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/** Maps domain failures to RFC 9457 (ex-7807) problem responses. */
@RestControllerAdvice
public class RunnerErrorHandler {

    @ExceptionHandler(RunQueueFullException.class)
    ResponseEntity<ProblemDetail> busy(RunQueueFullException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        pd.setType(URI.create("https://stackcraft.dev/problems/runner-busy"));
        pd.setTitle("Runner busy");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "2").body(pd);
    }

    @ExceptionHandler(SourceTooLargeException.class)
    ResponseEntity<ProblemDetail> tooLarge(SourceTooLargeException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, e.getMessage());
        pd.setTitle("Source too large");
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
    }
}
