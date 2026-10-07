package es.arrima.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
class HealthController {

    private final HealthService healthService;
    private final String version;

    /** Render sets RENDER_GIT_COMMIT to the commit of the running deploy. */
    HealthController(HealthService healthService, @Value("${RENDER_GIT_COMMIT:}") String commit) {
        this.healthService = healthService;
        this.version = commit.isBlank() ? "local" : commit.substring(0, Math.min(7, commit.length()));
    }

    /**
     * Liveness check used by Render and by the frontend while the server wakes up.
     * It does not touch the database, so it stays cheap even if Render calls it often.
     */
    @GetMapping
    HealthResponse liveness() {
        return HealthResponse.up(version);
    }

    /**
     * Called every 10 minutes by cron-job.org: the call keeps the Render instance awake
     * and the query keeps the Supabase project active.
     */
    @GetMapping("/db")
    HealthResponse database() {
        healthService.checkDatabase();
        return HealthResponse.up(version);
    }
}
