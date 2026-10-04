package es.arrima.schedule.domain;

import java.util.List;

/**
 * Who plays whom in one round (partida).
 *
 * @param byeTeam the team that rests this round, or null when the number of teams is even
 */
public record RoundPairings(int number, List<Pairing> pairings, Long byeTeam) {

    public RoundPairings {
        pairings = List.copyOf(pairings);
    }
}
