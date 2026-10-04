package es.arrima.international.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Whose turn it is in a round: first every team points its three balls, in team-number order; then
 * every team shoots its three, in the same order.
 */
public final class TurnOrder {

    private TurnOrder() {
    }

    /** A team in the round, in order of play. */
    public record Entry(long teamId, int teamSize) {
    }

    /** A ball already recorded. */
    public record ThrowKey(long teamId, ThrowKind kind, int ballNumber) {
    }

    public record Turn(long teamId, BallSlot slot) {
    }

    /** The next ball to throw, or empty when the round is complete. */
    public static Optional<Turn> next(List<Entry> teamsInOrder, Set<ThrowKey> recorded) {
        for (ThrowKind kind : ThrowKind.values()) {
            for (Entry team : teamsInOrder) {
                for (BallSlot slot : ThrowSequence.forTeamSize(team.teamSize())) {
                    if (slot.kind() == kind && !recorded.contains(new ThrowKey(team.teamId(), kind, slot.ballNumber()))) {
                        return Optional.of(new Turn(team.teamId(), slot));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
