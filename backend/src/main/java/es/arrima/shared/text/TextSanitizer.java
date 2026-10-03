package es.arrima.shared.text;

import java.text.Normalizer;

/**
 * Normalises free text typed or pasted by users (names from WhatsApp, the club name) before it is
 * validated and stored:
 * <ul>
 *   <li>Unicode NFC, so "é" typed in two different ways compares equal;</li>
 *   <li>control and invisible formatting characters removed (zero-width spaces, bidi overrides
 *       that can disguise text...), except the zero-width joiner that composite emojis need;</li>
 *   <li>any run of whitespace (including non-breaking spaces and line breaks) collapsed to one space.</li>
 * </ul>
 * This is not HTML escaping: the text is stored as plain text and React escapes it when rendering.
 */
public final class TextSanitizer {

    private static final int ZERO_WIDTH_JOINER = 0x200D;

    private TextSanitizer() {
    }

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC);
        StringBuilder result = new StringBuilder(normalized.length());
        boolean pendingSpace = false;
        for (int codePoint : normalized.codePoints().toArray()) {
            if (isWhitespace(codePoint)) {
                pendingSpace = true;
            } else if (!isInvisible(codePoint)) {
                if (pendingSpace && !result.isEmpty()) {
                    result.append(' ');
                }
                pendingSpace = false;
                result.appendCodePoint(codePoint);
            }
        }
        return result.toString();
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    private static boolean isInvisible(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONTROL
                || (type == Character.FORMAT && codePoint != ZERO_WIDTH_JOINER);
    }
}
