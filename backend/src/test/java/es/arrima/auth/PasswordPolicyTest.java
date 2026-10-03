package es.arrima.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.arrima.shared.error.ApiException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsLongEnoughPasswordsWithoutCompositionRules() {
        assertThatCode(() -> PasswordPolicy.check("bolas de acero", "password")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPasswords() {
        assertThatThrownBy(() -> PasswordPolicy.check("petanca", "password")).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsPasswordsBCryptWouldTruncate() {
        String seventyThreeBytes = "ñ".repeat(37); // 2 bytes each in UTF-8

        assertThatThrownBy(() -> PasswordPolicy.check(seventyThreeBytes, "password")).isInstanceOf(ApiException.class);
    }
}
