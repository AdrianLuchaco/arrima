package es.arrima.shared.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Best-effort address of the browser, for rate limiting only.
 * <p>
 * In production a request travels browser → Vercel → Render's proxy → application, and Vercel puts
 * the browser's address first in X-Forwarded-For. Anyone calling the Render URL directly can forge
 * that header, so limits keyed by IP are a coarse barrier, never an authorisation decision.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].strip();
        }
        return request.getRemoteAddr();
    }
}
