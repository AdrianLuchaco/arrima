package es.arrima;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestSecrets {

    private TestSecrets() {
    }

    public static String randomJwtSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }
}
