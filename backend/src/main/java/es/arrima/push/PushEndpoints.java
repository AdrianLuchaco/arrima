package es.arrima.push;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Which addresses we accept as push endpoints. The endpoint comes from the browser, that is, from
 * outside, and the server will send requests to it: without a list, anyone could make our server
 * call any address, including internal ones (SSRF). Only the push services of the browsers in use
 * are allowed: Chrome/Android (Google), Safari/iPhone (Apple), Firefox (Mozilla), Edge (Microsoft).
 */
final class PushEndpoints {

    static final int MAX_LENGTH = 1000;
    private static final Set<String> HOSTS = Set.of("fcm.googleapis.com", "web.push.apple.com", "updates.push.services.mozilla.com");
    private static final String MICROSOFT_SUFFIX = ".notify.windows.com";

    private PushEndpoints() {
    }

    static Optional<URI> parseAllowed(String endpoint) {
        if (endpoint == null || endpoint.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(endpoint);
            return isAllowed(uri) ? Optional.of(uri) : Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static boolean isAllowed(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean defaultPort = uri.getPort() == -1 || uri.getPort() == 443;
        return "https".equals(uri.getScheme()) && uri.getUserInfo() == null && defaultPort
                && (HOSTS.contains(host) || host.endsWith(MICROSOFT_SUFFIX));
    }
}
