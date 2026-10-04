package es.arrima.timer;

import java.time.Clock;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The server's clock, so each phone can correct its own: a phone a minute out would show a
 * countdown a minute wrong. The phone measures how long the request took and assumes half of it
 * was the way back (like NTP), which is precise enough for a countdown in seconds.
 */
@RestController
class ServerTimeController {

    private final Clock clock;

    ServerTimeController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/api/time")
    ResponseEntity<Map<String, Long>> now() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("now", clock.millis()));
    }
}
