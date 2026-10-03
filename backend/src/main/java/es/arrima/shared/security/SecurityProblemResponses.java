package es.arrima.shared.security;

import es.arrima.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Error bodies written by the security filters, which run before Spring MVC and therefore before
 * the ApiExceptionHandler. Same RFC 9457 shape and "code" property as every other error.
 */
final class SecurityProblemResponses {

    private SecurityProblemResponses() {
    }

    static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        int status = code.status().value();
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":%d,\"code\":\"%s\"}".formatted(status, code.name()));
    }
}
