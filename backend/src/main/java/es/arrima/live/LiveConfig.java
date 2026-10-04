package es.arrima.live;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling is only used for the SSE heartbeat, which matters only while someone is connected
 * (so the server is awake). Nothing that depends on time is scheduled: Render may be asleep.
 */
@Configuration
@EnableScheduling
class LiveConfig {
}
