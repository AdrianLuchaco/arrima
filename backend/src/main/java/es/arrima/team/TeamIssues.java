package es.arrima.team;

import java.util.List;

/**
 * Things that no longer add up after the teams were made (typically after going back to the
 * sign-up list and changing it). Shown to the admin; empty teams block the court schedule.
 *
 * @param unassignedPlayers   players who are in no team (arrived late, added or paid after the draw)
 * @param membersNotPlaying   people still in a team who no longer play (withdrawn, or recorded as
 *                            not paid): they need a substitute
 * @param teamsWithoutPlayers numbers of the teams that have nobody left to play
 */
public record TeamIssues(List<Long> unassignedPlayers, List<Long> membersNotPlaying, List<Integer> teamsWithoutPlayers) {

    public static final TeamIssues NONE = new TeamIssues(List.of(), List.of(), List.of());
}
