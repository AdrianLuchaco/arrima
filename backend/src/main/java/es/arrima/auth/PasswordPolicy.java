package es.arrima.auth;

import es.arrima.shared.error.ApiException;
import java.nio.charset.StandardCharsets;

/**
 * Length is what makes a password strong (NIST SP 800-63B): no rules about symbols or capitals,
 * which only lead to "Petanca1!". The upper limit is in bytes because BCrypt ignores everything
 * after 72 bytes, and an accent or an emoji takes more than one.
 */
final class PasswordPolicy {

    static final int MIN_CHARACTERS = 10;
    static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    static void check(String password, String field) {
        if (password == null || password.codePointCount(0, password.length()) < MIN_CHARACTERS) {
            throw ApiException.validation(field, "TooShort");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw ApiException.validation(field, "TooLong");
        }
    }
}
