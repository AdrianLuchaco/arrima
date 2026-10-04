package es.arrima.api;

import java.util.Map;

/**
 * Work that an operation would throw away. Teams or a schedule can always be redone; results,
 * Internacional throws and photos are real work, so losing them needs the admin's confirmation.
 */
record Losses(long results, long ballThrows, long photos) {

    boolean any() {
        return results > 0 || ballThrows > 0 || photos > 0;
    }

    Map<String, Object> asDetails() {
        return Map.of("losses", Map.of("results", results, "ballThrows", ballThrows, "photos", photos));
    }
}
