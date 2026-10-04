package es.arrima.schedule.domain;

/**
 * One line of the counter everyone keeps looking at.
 *
 * @param wins     the figure: all the rounds won, or one less
 * @param reached  teams that have exactly this many wins now
 * @param canReach teams below it that can still get there with the matches they have left
 */
public record WinTarget(int wins, long reached, long canReach) {
}
