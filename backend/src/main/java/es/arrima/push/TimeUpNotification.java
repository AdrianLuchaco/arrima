package es.arrima.push;

import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the phone shows when a round's time is up, in Spanish like the rest of the app. The service
 * worker displays it as it comes; tapping it opens the melee (the admin's screen, or the public view).
 */
final class TimeUpNotification {

    private TimeUpNotification() {
    }

    static byte[] payload(JsonMapper json, int roundNumber, String url, long meleeId) {
        return json.writeValueAsBytes(Map.of(
                "title", "¡Tiempo! Partida %d terminada".formatted(roundNumber),
                "body", "Quien vaya empatado juega una mano más.",
                "url", url,
                // The same tag replaces an earlier notification of the same round instead of piling up.
                "tag", "arrima-time-up-%d-%d".formatted(meleeId, roundNumber)));
    }
}
