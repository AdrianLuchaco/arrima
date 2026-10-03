package es.arrima.shared.security;

import es.arrima.shared.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF protection for the only endpoints that authenticate with a cookie (the refresh token).
 * <p>
 * The cookie is SameSite=Strict, so browsers do not send it on requests started by another site.
 * As a second layer, this filter rejects requests that the browser itself labels as not coming
 * from our own origin (the Sec-Fetch-Site header, sent by every browser we support). Requests
 * without the header are not from a browser, so they cannot carry a victim's cookie.
 */
class CrossSiteRequestFilter extends OncePerRequestFilter {

    private static final Set<String> COOKIE_ENDPOINTS = Set.of("/api/auth/refresh", "/api/auth/logout");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !COOKIE_ENDPOINTS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (fetchSite != null && !fetchSite.equals("same-origin")) {
            SecurityProblemResponses.write(response, ErrorCode.CROSS_SITE_REQUEST);
            return;
        }
        chain.doFilter(request, response);
    }
}
