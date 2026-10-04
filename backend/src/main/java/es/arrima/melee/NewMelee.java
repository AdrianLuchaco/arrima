package es.arrima.melee;

/**
 * What the admin chooses when creating a melee. Null settings mean "use the club's default".
 */
public record NewMelee(int teamSize, Integer courtCount, Integer roundsCount, Integer prizeCount, Integer entryFeeCents) {
}
