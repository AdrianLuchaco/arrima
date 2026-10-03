package es.arrima.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * HMAC keys derived from JWT_SECRET. Each use gets its own key (domain separation), so a value
 * signed for one purpose can never be accepted for another, and there is one secret less to manage.
 */
@Component
public class SigningKeys {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey jwtKey;
    private final SecretKey fileLinkKey;

    public SigningKeys(SecurityProperties properties) {
        byte[] secret = decode(properties.jwtSecret());
        this.jwtKey = new SecretKeySpec(derive(secret, "arrima-jwt"), HMAC_SHA256);
        this.fileLinkKey = new SecretKeySpec(derive(secret, "arrima-file-links"), HMAC_SHA256);
    }

    public SecretKey jwtKey() {
        return jwtKey;
    }

    public SecretKey fileLinkKey() {
        return fileLinkKey;
    }

    private static byte[] decode(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set (see backend/.env.example)");
        }
        byte[] secret = Base64.getDecoder().decode(base64Secret.strip());
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be base64 of at least 32 random bytes");
        }
        return secret;
    }

    private static byte[] derive(byte[] secret, String purpose) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            return mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
