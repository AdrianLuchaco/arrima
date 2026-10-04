package es.arrima.api.view;

import es.arrima.international.BallThrow;
import es.arrima.international.InternationalState;
import es.arrima.international.InternationalState.GroupState;
import es.arrima.international.InternationalState.RoundState;
import es.arrima.international.RoundTeam;
import es.arrima.international.domain.FinalRanking.RankedTeam;
import es.arrima.international.domain.ThrowSequence;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Turns the Internacional's state into its part of the melee view. */
final class InternationalViews {

    private InternationalViews() {
    }

    static MeleeView.International of(InternationalState state, Map<Long, Integer> teamSizes) {
        Map<Long, Integer> mainRoundPoints = new HashMap<>();
        Set<Long> decidedByTieBreak = new HashSet<>();
        for (GroupState group : state.groups()) {
            for (RoundState round : group.rounds()) {
                for (RoundTeam team : round.round().getTeams()) {
                    if (round.round().getRoundNumber() == 1) {
                        mainRoundPoints.put(team.teamId(), round.pointsOf(team.teamId()));
                    } else if (!round.obsolete()) {
                        decidedByTieBreak.add(team.teamId());
                    }
                }
            }
        }

        List<MeleeView.Ranked> assured = state.plan().stream()
                .filter(group -> !group.needsPlay())
                .map(group -> new MeleeView.Ranked(group.bestPosition(), group.teamIds().getFirst(), null, false))
                .toList();
        List<MeleeView.Ranked> finalRanking = state.finalRanking().stream()
                .map((RankedTeam ranked) -> new MeleeView.Ranked(ranked.position(), ranked.teamId(),
                        mainRoundPoints.get(ranked.teamId()), decidedByTieBreak.contains(ranked.teamId())))
                .toList();
        MeleeView.Turn turn = state.turn() == null ? null : new MeleeView.Turn(state.turn().groupId(),
                state.turn().roundId(), state.turn().teamId(), state.turn().slot().kind(), state.turn().slot().ballNumber(),
                state.turn().slot().position());

        return new MeleeView.International(
                state.groups().stream().map(group -> groupView(group, teamSizes)).toList(),
                assured, turn, finalRanking, state.complete());
    }

    private static MeleeView.IntlGroup groupView(GroupState group, Map<Long, Integer> teamSizes) {
        return new MeleeView.IntlGroup(group.group().getId(), group.group().getPlayOrder(), group.group().getWins(),
                group.group().getBestPrizePosition(), group.group().getWorstPrizePosition(), group.group().getStatus().name(),
                group.standing().order(), group.hasObsoletePlayedRound(),
                group.rounds().stream().map(round -> roundView(round, teamSizes)).toList());
    }

    private static MeleeView.IntlRound roundView(RoundState round, Map<Long, Integer> teamSizes) {
        List<MeleeView.IntlTeam> teams = round.round().getTeams().stream()
                .map(team -> new MeleeView.IntlTeam(team.teamId(), team.playOrder(), round.pointsOf(team.teamId()),
                        round.ballThrows().stream()
                                .filter(ball -> ball.getTeamId() == team.teamId())
                                .sorted(Comparator.comparing(BallThrow::getKind).thenComparingInt(BallThrow::getBallNumber))
                                .map(ball -> ballView(ball, teamSizes.getOrDefault(team.teamId(), 2)))
                                .toList()))
                .toList();
        return new MeleeView.IntlRound(round.round().getId(), round.round().getRoundNumber(), round.obsolete(),
                round.complete(), teams);
    }

    private static MeleeView.Ball ballView(BallThrow ball, int teamSize) {
        return new MeleeView.Ball(ball.getKind(), ball.getBallNumber(),
                ThrowSequence.slot(teamSize, ball.getKind(), ball.getBallNumber()).position(),
                ball.getOutcome(), ball.getPoints(), ball.isCorrected());
    }
}
