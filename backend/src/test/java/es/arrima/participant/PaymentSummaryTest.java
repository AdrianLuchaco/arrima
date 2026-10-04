package es.arrima.participant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentSummaryTest {

    @Test
    void countsWhoPaidAndTheMoneyCollected() {
        List<Participant> people = List.of(
                person(PaymentStatus.PAID), person(PaymentStatus.PAID), person(PaymentStatus.UNPAID),
                person(PaymentStatus.UNMARKED));

        assertThat(PaymentSummary.of(people, 500)).isEqualTo(new PaymentSummary(4, 2, 1, 1, 500, 1000));
    }

    @Test
    void peopleWhoDidNotComeAreNotExpectedToPay() {
        Participant withdrawn = person(PaymentStatus.PAID);
        withdrawn.withdraw();

        assertThat(PaymentSummary.of(List.of(withdrawn, person(PaymentStatus.UNMARKED)), 500))
                .isEqualTo(new PaymentSummary(1, 0, 0, 1, 500, 0));
    }

    @Test
    void onlyThoseWhoCameAndPaidPlayWhenThereIsAFee() {
        Participant paid = person(PaymentStatus.PAID);
        Participant unmarked = person(PaymentStatus.UNMARKED);
        Participant withdrawn = person(PaymentStatus.PAID);
        withdrawn.withdraw();

        assertThat(paid.plays(true)).isTrue();
        assertThat(unmarked.plays(true)).isFalse();
        assertThat(withdrawn.plays(true)).isFalse();
        // Without a fee, as before the payment control: everyone who came plays.
        assertThat(unmarked.plays(false)).isTrue();
        assertThat(withdrawn.plays(false)).isFalse();
    }

    private static Participant person(PaymentStatus status) {
        Participant participant = new Participant(1, null, "Jugador", Instant.EPOCH);
        participant.recordPayment(status);
        return participant;
    }
}
