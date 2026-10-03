package es.arrima.health;

/**
 * Body returned by the health endpoints.
 */
public record HealthResponse(String status) {

    static HealthResponse up() {
        return new HealthResponse("UP");
    }
}
