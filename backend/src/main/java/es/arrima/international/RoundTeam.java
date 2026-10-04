package es.arrima.international;

import jakarta.persistence.Embeddable;

/** A team taking part in a round, and its turn within it (by team number). */
@Embeddable
public record RoundTeam(long teamId, int playOrder) {
}
