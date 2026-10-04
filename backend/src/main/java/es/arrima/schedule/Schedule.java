package es.arrima.schedule;

import es.arrima.schedule.domain.MatchResult;
import es.arrima.schedule.domain.Standings;
import es.arrima.schedule.domain.TeamRecord;
import es.arrima.schedule.domain.WinTarget;
import java.util.Collection;
import java.util.List;

/** The court schedule of a melee as stored, with the standings derived from it. */
public record Schedule(List<Matchup> matchups, List<Bye> byes) {

    public boolean exists() {
        return !matchups.isEmpty() || !byes.isEmpty();
    }

    public long pendingResults() {
        return matchups.stream().filter(matchup -> !matchup.isDecided()).count();
    }

    public List<TeamRecord> records(Collection<Long> teamIds) {
        List<MatchResult> results = matchups.stream()
                .map(matchup -> new MatchResult(matchup.getTeamAId(), matchup.getTeamBId(), matchup.getWinnerTeamId()))
                .toList();
        return Standings.records(teamIds, results, byes.stream().map(Bye::getTeamId).toList());
    }

    public List<WinTarget> counter(Collection<Long> teamIds, int rounds) {
        return Standings.counter(records(teamIds), rounds);
    }
}
