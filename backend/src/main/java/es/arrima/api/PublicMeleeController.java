package es.arrima.api;

import es.arrima.api.view.Audience;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.live.MeleeEventBroadcaster;
import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeRepository;
import es.arrima.melee.MeleeService;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.ratelimit.RateLimitPolicy;
import es.arrima.shared.ratelimit.RateLimiter;
import es.arrima.shared.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * What players and spectators see with the code or the QR, without an account. Only GET methods
 * exist here, and the security configuration only lets GET through: nothing public can write.
 */
@RestController
@RequestMapping("/api/public/melees/{code}")
class PublicMeleeController {

    private static final Pattern CODE_FORMAT = Pattern.compile("[2-9A-Z]{8}");

    private final MeleeAccess meleeAccess;
    private final MeleeRepository meleeRepository;
    private final MeleeViewAssembler views;
    private final MeleeEventBroadcaster broadcaster;
    private final RateLimiter rateLimiter;
    private final MeleeService meleeService;

    PublicMeleeController(MeleeAccess meleeAccess, MeleeRepository meleeRepository, MeleeViewAssembler views,
            MeleeEventBroadcaster broadcaster, RateLimiter rateLimiter, MeleeService meleeService) {
        this.meleeAccess = meleeAccess;
        this.meleeRepository = meleeRepository;
        this.views = views;
        this.broadcaster = broadcaster;
        this.rateLimiter = rateLimiter;
        this.meleeService = meleeService;
    }

    /**
     * The view with an ETag made of its revision. Phones poll it when the live connection is down;
     * if nothing changed they get a 304 without a body, which matters on a weak signal.
     */
    @GetMapping
    ResponseEntity<MeleeView> view(@PathVariable String code, HttpServletRequest http, WebRequest request) {
        meleeService.closeIfIdle(lookUp(code, http).getId());
        Melee melee = lookUp(code, http);
        String etag = "\"r" + meleeRepository.findRevision(melee.getId()) + "\"";
        if (request.checkNotModified(etag)) {
            return null; // Spring has already answered 304 Not Modified
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.noCache())
                .body(views.assemble(melee, Audience.PUBLIC));
    }

    @GetMapping("/events")
    ResponseEntity<SseEmitter> events(@PathVariable String code, HttpServletRequest http) {
        Melee melee = lookUp(code, http);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                // Ask proxies not to buffer the stream (nginx and others honour it).
                .header("X-Accel-Buffering", "no")
                .body(broadcaster.subscribe(melee.getId()));
    }

    /**
     * Only failed look-ups count against the limit: guessing codes gets blocked quickly, while a whole
     * club watching from the same mobile carrier address is never bothered.
     */
    private Melee lookUp(String rawCode, HttpServletRequest http) {
        String ip = ClientIp.of(http);
        rateLimiter.ensureNotExceeded(RateLimitPolicy.PUBLIC_CODE_MISS_PER_IP, ip);
        String code = rawCode.strip().toUpperCase(Locale.ROOT);
        return (CODE_FORMAT.matcher(code).matches() ? meleeAccess.byPublicCode(code) : Optional.<Melee>empty())
                .orElseThrow(() -> {
                    rateLimiter.consume(RateLimitPolicy.PUBLIC_CODE_MISS_PER_IP, ip);
                    return ApiException.notFound();
                });
    }
}
