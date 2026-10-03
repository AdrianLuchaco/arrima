package es.arrima.health;

import org.springframework.stereotype.Service;

@Service
class HealthService {

    private final DatabaseProbeRepository databaseProbeRepository;

    HealthService(DatabaseProbeRepository databaseProbeRepository) {
        this.databaseProbeRepository = databaseProbeRepository;
    }

    /**
     * Supabase pauses free projects after 7 days without database activity, so this runs a real
     * query against an application table instead of only validating a connection.
     * If the database is unreachable the exception reaches the client as a 503.
     */
    void checkDatabase() {
        databaseProbeRepository.countClubs();
    }
}
