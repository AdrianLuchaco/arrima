package es.arrima.international.domain;

/** Where a ball ended up, as marked on the drawing of the field. Each outcome belongs to one kind of throw. */
public enum ThrowOutcome {

    // Pointing: a jack in the centre of two concentric circles.
    OUT(ThrowKind.POINTING),           // outside both circles
    BIG_CIRCLE(ThrowKind.POINTING),    // in the big circle or touching its line
    SMALL_CIRCLE(ThrowKind.POINTING),  // in the small circle or touching its line
    NEAR_JACK(ThrowKind.POINTING),     // less than 10 cm from the jack
    ON_JACK(ThrowKind.POINTING),       // touching the jack ("chupete")

    // Shooting: a ball in the centre and an iron ring on the small circle's line.
    MISS(ThrowKind.SHOOTING),          // no hit, bounced outside the small circle first, or the iron rang
    HIT(ThrowKind.SHOOTING),           // hit, the target ball stays inside the small circle
    HIT_OUT(ThrowKind.SHOOTING),       // hit, the target ball leaves the small circle
    CARREAU(ThrowKind.SHOOTING);       // "carro": hit, and the thrown ball stays inside the small circle

    private final ThrowKind kind;

    ThrowOutcome(ThrowKind kind) {
        this.kind = kind;
    }

    public ThrowKind kind() {
        return kind;
    }
}
