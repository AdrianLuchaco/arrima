package es.arrima.shared.text;

import es.arrima.shared.error.ApiException;

/**
 * Cleans user text with {@link TextSanitizer} and then checks it, because what matters is the
 * length after removing invisible characters and extra spaces, not before.
 * The reasons ("NotBlank", "Size") match Bean Validation's, so the frontend handles both alike.
 */
public final class TextInput {

    private TextInput() {
    }

    public static String require(String raw, String field, int maxLength) {
        String text = TextSanitizer.clean(raw);
        if (text.isEmpty()) {
            throw ApiException.validation(field, "NotBlank");
        }
        if (text.codePointCount(0, text.length()) > maxLength) {
            throw ApiException.validation(field, "Size");
        }
        return text;
    }
}
