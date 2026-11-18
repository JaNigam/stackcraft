package dev.stackcraft.api.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Correlation id: taken from {@code X-Request-Id} (or created), logged on every line, forwarded to the runner. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(HEADER);
        if (id == null || !SAFE.matcher(id).matches()) id = UUID.randomUUID().toString();
        MDC.put("requestId", id);
        res.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("requestId");
        }
    }
}
