package es.arrima.participant;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Comparison key for names: "José  García", "jose garcia" and "JOSÉ GARCÍA 👍" are the same person.
 * Used only to warn about likely duplicates, never to merge anyone automatically.
 */
final class NameKey {

    private NameKey() {
    }

    static String of(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }
}
