package es.arrima.team;

import java.util.List;

/**
 * Things that no longer add up after the teams were made (typically after going back to the
 * sign-up list and changing it). Shown to the admin; empty teams block the court schedule.
 *
 * @param unassignedPlayers active players who are in no team (arrived late, or added after the draw)
 * @param withdrawnMembers  withdrawn players still in a team: they need a substitute
 * @param teamsWithoutActivePlayers numbers of the teams that have nobody left to play
 */
public record TeamIssues(List<Long> unassignedPlayers, List<Long> withdrawnMembers, List<Integer> teamsWithoutActivePlayers) {

    public static final TeamIssues NONE = new TeamIssues(List.of(), List.of(), List.of());
}
