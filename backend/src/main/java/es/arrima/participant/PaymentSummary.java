package es.arrima.participant;

import java.util.List;

/**
 * The figures at the sign-up table: "34 de 35 han pagado · 170 €", and how many are still unmarked.
 * Withdrawn people (did not come) are not expected to pay, so they are left out of every count.
 *
 * @param expected       active people, the ones who should pay
 * @param collectedCents paid × entry fee
 */
public record PaymentSummary(int expected, int paid, int unpaid, int unmarked, int entryFeeCents, long collectedCents) {

    public static PaymentSummary of(List<Participant> participants, int entryFeeCents) {
        List<Participant> active = participants.stream().filter(Participant::isActive).toList();
        int paid = count(active, PaymentStatus.PAID);
        return new PaymentSummary(active.size(), paid, count(active, PaymentStatus.UNPAID),
                count(active, PaymentStatus.UNMARKED), entryFeeCents, (long) paid * entryFeeCents);
    }

    private static int count(List<Participant> people, PaymentStatus status) {
        return (int) people.stream().filter(participant -> participant.getPaymentStatus() == status).count();
    }
}
