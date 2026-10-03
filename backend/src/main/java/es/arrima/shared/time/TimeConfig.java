package es.arrima.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfig {

    /** Every "now" in the application comes from this clock, so tests can move time forward. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
