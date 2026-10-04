package es.arrima.api;

import es.arrima.api.MeleeRequests.SubscribeToPush;
import es.arrima.melee.MeleeAccess;
import es.arrima.push.PushAudience;
import es.arrima.push.PushKeys;
import es.arrima.push.PushSubscriptionService;
import es.arrima.push.VapidKeys;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.security.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Web Push: the server's public key for the browsers, and the admin's own subscription. */
@RestController
class PushController {

    private final PushKeys pushKeys;
    private final PushSubscriptionService pushSubscriptions;
    private final MeleeAccess meleeAccess;

    PushController(PushKeys pushKeys, PushSubscriptionService pushSubscriptions, MeleeAccess meleeAccess) {
        this.pushKeys = pushKeys;
        this.pushSubscriptions = pushSubscriptions;
        this.meleeAccess = meleeAccess;
    }

    /** 404 when notifications are off (no VAPID keys): the app then simply does not offer them. */
    @GetMapping("/api/public/push/key")
    Map<String, String> publicKey() {
        VapidKeys keys = pushKeys.keys().orElseThrow(ApiException::notFound);
        return Map.of("publicKey", keys.publicKeyBase64Url());
    }

    /** The admin's phone, the official alarm of the table. */
    @PostMapping("/api/melees/{meleeId}/push-subscriptions")
    ResponseEntity<Void> subscribeAdmin(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody SubscribeToPush request) {
        pushSubscriptions.subscribe(meleeAccess.forClub(meleeId, admin.clubId()), request.toNewSubscription(),
                PushAudience.ADMIN);
        return ResponseEntity.noContent().build();
    }
}
