package es.arrima.push;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The VAPID keys, if configured. Notifications are an extra: without keys the app works the same
 * (the countdown and the alarm in open apps don't need them), so the backend starts and says so in
 * the log. Keys that are set but wrong do stop the start-up: that is a mistake to fix.
 */
@Component
@EnableConfigurationProperties(PushProperties.class)
public class PushKeys {

    private static final Logger log = LoggerFactory.getLogger(PushKeys.class);

    private final VapidKeys keys;
    private final String subject;

    PushKeys(PushProperties properties) {
        if (isBlank(properties.vapidPublicKey()) && isBlank(properties.vapidPrivateKey())) {
            log.warn("Web Push is off: VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY are not set");
            this.keys = null;
            this.subject = null;
            return;
        }
        if (isBlank(properties.vapidPublicKey()) || isBlank(properties.vapidPrivateKey()) || isBlank(properties.subject())) {
            throw new IllegalStateException("VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY and VAPID_SUBJECT go together");
        }
        this.keys = VapidKeys.parse(properties.vapidPublicKey(), properties.vapidPrivateKey());
        this.subject = properties.subject().strip();
    }

    public Optional<VapidKeys> keys() {
        return Optional.ofNullable(keys);
    }

    String subject() {
        return subject;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
