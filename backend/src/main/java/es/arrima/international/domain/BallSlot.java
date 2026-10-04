package es.arrima.international.domain;

/** One of the six balls of a team in a round: which kind, which of the three, and who throws it. */
public record BallSlot(ThrowKind kind, int ballNumber, PlayerPosition position) {
}
