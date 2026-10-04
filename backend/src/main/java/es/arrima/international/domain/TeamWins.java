package es.arrima.international.domain;

/** A team's wins after the matches; the number orders teams within a group. */
public record TeamWins(long teamId, int teamNumber, int wins) {
}
