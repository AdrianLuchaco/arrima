package es.arrima.files;

import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.shared.security.SigningKeys;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import org.springframework.stereotype.Component;

/**
 * Signed, expiring links to private images: /api/files/{path}?expires=...&signature=...
 * Whoever receives the link (the admin, or a spectator with the melee code) can see the image;
 * nobody can build a link to another image without the key.
 */
@Component
public class FileLinkSigner {

    /**
     * Expiry is rounded up to the next 6-hour boundary plus 6 hours (so 6 to 12 hours of validity).
     * The same image then keeps the same URL across refreshes, and the browser cache works.
     */
    private static final long STEP_SECONDS = Duration.ofHours(6).toSeconds();

    private final SigningKeys signingKeys;
    private final Clock clock;

    public FileLinkSigner(SigningKeys signingKeys, Clock clock) {
        this.signingKeys = signingKeys;
        this.clock = clock;
    }

    /** Returns null for a null path, so optional images (a club without logo) map naturally. */
    public String link(String path) {
        if (path == null) {
            return null;
        }
        long now = clock.instant().getEpochSecond();
        long expires = (now / STEP_SECONDS + 2) * STEP_SECONDS;
        return "/api/files/%s?expires=%d&signature=%s".formatted(path, expires, signature(path, expires));
    }

    void verify(String path, long expires, String signature) {
        boolean expired = clock.instant().getEpochSecond() > expires;
        boolean valid = MessageDigest.isEqual(
                signature(path, expires).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
        if (expired || !valid) {
            throw new ApiException(ErrorCode.INVALID_FILE_LINK);
        }
    }

    private String signature(String path, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKeys.fileLinkKey());
            byte[] digest = mac.doFinal((path + "\n" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
