package es.arrima.schedule.domain;

import java.util.Collection;
import java.util.List;

/** Wins of each team and the counter of the club's paper sheet, computed from the results. */
public final class Standings {

    private Standings() {
    }

    public static List<TeamRecord> records(Collection<Long> teamIds, List<MatchResult> matches, Collection<Long> byeTeams) {
        return teamIds.stream().map(team -> recordOf(team, matches, byeTeams)).toList();
    }

    /**
     * The counter, with the club's reading: for "all rounds won" and "one less", how many teams
     * have exactly that many wins now, and how many more can still reach it.
     * Example with 4 rounds: "4 wins: 2 teams (5 more can reach it) · 3 wins: 3 teams (8 more can)".
     */
    public static List<WinTarget> counter(List<TeamRecord> records, int rounds) {
        return List.of(target(records, rounds), target(records, rounds - 1)).stream()
                .filter(target -> target.wins() > 0)
                .toList();
    }

    private static WinTarget target(List<TeamRecord> records, int wins) {
        long reached = records.stream().filter(record -> record.wins() == wins).count();
        long canReach = records.stream()
                .filter(record -> record.wins() < wins && record.maxPossibleWins() >= wins)
                .count();
        return new WinTarget(wins, reached, canReach);
    }

    private static TeamRecord recordOf(long team, List<MatchResult> matches, Collection<Long> byeTeams) {
        int wins = byeTeams.contains(team) ? 1 : 0;
        int losses = 0;
        int pending = 0;
        for (MatchResult match : matches) {
            if (!match.involves(team)) {
                continue;
            }
            if (!match.isDecided()) {
                pending++;
            } else if (match.winner() == team) {
                wins++;
            } else {
                losses++;
            }
        }
        return new TeamRecord(team, wins, losses, pending);
    }
}
