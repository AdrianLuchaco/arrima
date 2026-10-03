package es.arrima.api.view;

import es.arrima.melee.MeleeStatus;
import java.time.LocalDate;

/** One line of the club's melee list (current ones and history). */
public record MeleeSummary(long id, LocalDate playedOn, MeleeStatus status, int teamSize, String publicCode, long activePlayers) {
}
