package es.arrima.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
class HealthController {

    private final HealthService healthService;

    HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Liveness check used by Render and by the frontend while the server wakes up.
     * It does not touch the database, so it stays cheap even if Render calls it often.
     */
    @GetMapping
    HealthResponse liveness() {
        return HealthResponse.up();
    }

    /**
     * Called every 10 minutes by cron-job.org: the call keeps the Render instance awake
     * and the query keeps the Supabase project active.
     */
    @GetMapping("/db")
    HealthResponse database() {
        healthService.checkDatabase();
        return HealthResponse.up();
    }
}
