package es.arrima.push;

import es.arrima.shared.http.OutboundHttp;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PushConfig {

    /** Without VAPID keys there can be no subscriptions, so nothing is ever sent. */
    @Bean
    PushGateway pushGateway(PushKeys pushKeys, Clock clock) {
        return pushKeys.keys()
                .<PushGateway>map(keys -> new WebPushSender(OutboundHttp.restClient(), keys, pushKeys.subject(), clock))
                .orElse((subscription, payload) -> PushGateway.Result.FAILED);
    }
}
