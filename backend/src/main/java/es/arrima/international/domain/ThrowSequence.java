package es.arrima.international.domain;

import static es.arrima.international.domain.PlayerPosition.MIDDLE;
import static es.arrima.international.domain.PlayerPosition.POINTER;
import static es.arrima.international.domain.PlayerPosition.SHOOTER;
import static es.arrima.international.domain.ThrowKind.POINTING;
import static es.arrima.international.domain.ThrowKind.SHOOTING;

import java.util.List;

/**
 * Who throws each of a team's six balls (three pointing, three shooting):
 * <ul>
 *   <li>doublette: the pointer points all three, the shooter shoots all three;</li>
 *   <li>triplette: each player throws two. Pointing: punta, punta, middle. Shooting: middle,
 *       shooter, shooter.</li>
 * </ul>
 * An odd-sized team (a triplette among doublettes, or the other way round) follows its own size.
 * The screen shows the position; who holds each position is not recorded.
 */
public final class ThrowSequence {

    public static final int BALLS_PER_KIND = 3;

    private static final List<BallSlot> DOUBLETTE = List.of(
            new BallSlot(POINTING, 1, POINTER), new BallSlot(POINTING, 2, POINTER), new BallSlot(POINTING, 3, POINTER),
            new BallSlot(SHOOTING, 1, SHOOTER), new BallSlot(SHOOTING, 2, SHOOTER), new BallSlot(SHOOTING, 3, SHOOTER));

    private static final List<BallSlot> TRIPLETTE = List.of(
            new BallSlot(POINTING, 1, POINTER), new BallSlot(POINTING, 2, POINTER), new BallSlot(POINTING, 3, MIDDLE),
            new BallSlot(SHOOTING, 1, MIDDLE), new BallSlot(SHOOTING, 2, SHOOTER), new BallSlot(SHOOTING, 3, SHOOTER));

    private ThrowSequence() {
    }

    public static List<BallSlot> forTeamSize(int teamSize) {
        return switch (teamSize) {
            case 2 -> DOUBLETTE;
            case 3 -> TRIPLETTE;
            default -> throw new IllegalArgumentException("La Internacional is for teams of 2 or 3, not " + teamSize);
        };
    }

    public static BallSlot slot(int teamSize, ThrowKind kind, int ballNumber) {
        return forTeamSize(teamSize).stream()
                .filter(slot -> slot.kind() == kind && slot.ballNumber() == ballNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No ball %s %d".formatted(kind, ballNumber)));
    }
}
