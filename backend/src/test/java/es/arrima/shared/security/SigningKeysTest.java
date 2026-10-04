package es.arrima.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SigningKeysTest {

    private static final byte[] RANDOM_BYTES = new byte[32];

    static {
        new SecureRandom().nextBytes(RANDOM_BYTES);
    }

    @Test
    void acceptsBothBase64AlphabetsForTheSameKey() {
        SigningKeys standard = keysFor(Base64.getEncoder().encodeToString(RANDOM_BYTES));
        SigningKeys urlSafe = keysFor(Base64.getUrlEncoder().encodeToString(RANDOM_BYTES));

        assertThat(urlSafe.jwtKey().getEncoded()).isEqualTo(standard.jwtKey().getEncoded());
    }

    @Test
    void eachUseHasItsOwnKey() {
        SigningKeys keys = keysFor(Base64.getEncoder().encodeToString(RANDOM_BYTES));

        assertThat(keys.fileLinkKey().getEncoded()).isNotEqualTo(keys.jwtKey().getEncoded());
    }

    @Test
    void refusesToStartWithAShortOrMissingSecret() {
        assertThatThrownBy(() -> keysFor(Base64.getEncoder().encodeToString(new byte[16])))
                .hasMessageContaining("at least 32");
        assertThatThrownBy(() -> keysFor("")).hasMessageContaining("not set");
        assertThatThrownBy(() -> keysFor("esto no es base64!")).hasMessageContaining("not valid base64");
    }

    private static SigningKeys keysFor(String secret) {
        return new SigningKeys(new SecurityProperties(secret, Duration.ofMinutes(15), Duration.ofDays(60),
                Duration.ofSeconds(60), true));
    }
}
