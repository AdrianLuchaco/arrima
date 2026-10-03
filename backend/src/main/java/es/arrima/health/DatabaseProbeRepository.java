package es.arrima.health;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class DatabaseProbeRepository {

    private final JdbcClient jdbcClient;

    DatabaseProbeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    long countClubs() {
        return jdbcClient.sql("select count(*) from club")
                .query(Long.class)
                .single();
    }
}
