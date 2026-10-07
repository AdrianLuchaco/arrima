package es.arrima.health;

/**
 * Body returned by the health endpoints.
 *
 * @param version the commit being run (first 7 characters), or "local" outside Render: shows from
 *                outside whether a push has really been deployed. The code is public, so this
 *                reveals nothing new.
 */
public record HealthResponse(String status, String version) {

    static HealthResponse up(String version) {
        return new HealthResponse("UP", version);
    }
}
