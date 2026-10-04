package es.arrima.international.domain;

import static es.arrima.international.domain.PlayerPosition.MIDDLE;
import static es.arrima.international.domain.PlayerPosition.POINTER;
import static es.arrima.international.domain.PlayerPosition.SHOOTER;
import static es.arrima.international.domain.ThrowKind.POINTING;
import static es.arrima.international.domain.ThrowKind.SHOOTING;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.international.domain.TurnOrder.Entry;
import es.arrima.international.domain.TurnOrder.ThrowKey;
import es.arrima.international.domain.TurnOrder.Turn;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ThrowSequenceAndTurnOrderTest {

    @Test
    void inADoubletteThePointerPointsAndTheShooterShoots() {
        assertThat(ThrowSequence.forTeamSize(2)).extracting(BallSlot::position)
                .containsExactly(POINTER, POINTER, POINTER, SHOOTER, SHOOTER, SHOOTER);
    }

    @Test
    void inATripletteEachPlayerThrowsTwo() {
        assertThat(ThrowSequence.forTeamSize(3)).containsExactly(
                new BallSlot(POINTING, 1, POINTER), new BallSlot(POINTING, 2, POINTER), new BallSlot(POINTING, 3, MIDDLE),
                new BallSlot(SHOOTING, 1, MIDDLE), new BallSlot(SHOOTING, 2, SHOOTER), new BallSlot(SHOOTING, 3, SHOOTER));
    }

    @Test
    void everyTeamPointsBeforeAnyTeamShoots() {
        List<Entry> teams = List.of(new Entry(7, 2), new Entry(9, 3));
        Set<ThrowKey> recorded = new HashSet<>();

        List<String> order = new ArrayList<>();
        for (Optional<Turn> turn = TurnOrder.next(teams, recorded); turn.isPresent(); turn = TurnOrder.next(teams, recorded)) {
            Turn t = turn.get();
            order.add(t.teamId() + " " + t.slot().kind() + " " + t.slot().ballNumber() + " " + t.slot().position());
            recorded.add(new ThrowKey(t.teamId(), t.slot().kind(), t.slot().ballNumber()));
        }

        assertThat(order).containsExactly(
                "7 POINTING 1 POINTER", "7 POINTING 2 POINTER", "7 POINTING 3 POINTER",
                "9 POINTING 1 POINTER", "9 POINTING 2 POINTER", "9 POINTING 3 MIDDLE",
                "7 SHOOTING 1 SHOOTER", "7 SHOOTING 2 SHOOTER", "7 SHOOTING 3 SHOOTER",
                "9 SHOOTING 1 MIDDLE", "9 SHOOTING 2 SHOOTER", "9 SHOOTING 3 SHOOTER");
    }

    @Test
    void aBallRecordedOutOfOrderIsSkipped() {
        List<Entry> teams = List.of(new Entry(7, 2));
        Set<ThrowKey> recorded = Set.of(new ThrowKey(7, POINTING, 1), new ThrowKey(7, POINTING, 3));

        assertThat(TurnOrder.next(teams, recorded)).hasValue(new Turn(7, new BallSlot(POINTING, 2, POINTER)));
    }
}
