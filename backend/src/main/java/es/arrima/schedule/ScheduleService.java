package es.arrima.schedule;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeStatus;
import es.arrima.participant.ParticipantService;
import es.arrima.schedule.domain.PairingStrategy;
import es.arrima.schedule.domain.RandomPairing;
import es.arrima.schedule.domain.ScheduleGenerator;
import es.arrima.schedule.domain.ScheduleLimits;
import es.arrima.schedule.domain.ScheduledMatch;
import es.arrima.schedule.domain.ScheduledRound;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.team.Team;
import es.arrima.team.TeamIssues;
import es.arrima.team.TeamService;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final MatchupRepository matchupRepository;
    private final ByeRepository byeRepository;
    private final TeamService teamService;
    private final ParticipantService participantService;
    private final MeleeAccess meleeAccess;
    private final Clock clock;
    private final RandomGenerator random = new SecureRandom();
    private final PairingStrategy randomPairing = new RandomPairing();

    public ScheduleService(MatchupRepository matchupRepository, ByeRepository byeRepository, TeamService teamService,
            ParticipantService participantService, MeleeAccess meleeAccess, Clock clock) {
        this.matchupRepository = matchupRepository;
        this.byeRepository = byeRepository;
        this.teamService = teamService;
        this.participantService = participantService;
        this.meleeAccess = meleeAccess;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Schedule scheduleOf(long meleeId) {
        return new Schedule(matchupRepository.findByMeleeIdOrderByRoundNumberAscCourtNumberAscIdAsc(meleeId),
                byeRepository.findByMeleeIdOrderByRoundNumber(meleeId));
    }

    /**
     * "Generar cuadro": every pairing of every round is fixed now, as on the paper sheet, and does not
     * depend on who wins. Replaces any previous schedule (MeleeWorkflow asks before losing results).
     */
    @Transactional
    public void generate(Melee melee) {
        melee.requireStatus(MeleeStatus.TEAMS);
        List<Team> teams = teamService.teamsOf(melee.getId());
        if (teams.size() < 2) {
            throw new ApiException(ErrorCode.NO_TEAMS);
        }
        TeamIssues issues = teamService.issues(melee, teams, participantService.listForMelee(melee.getId()));
        if (!issues.teamsWithoutPlayers().isEmpty()) {
            throw new ApiException(ErrorCode.TEAMS_INCOMPLETE, Map.of("teams", issues.teamsWithoutPlayers()));
        }
        int rounds = melee.getSettings().roundsCount();
        int maxRounds = ScheduleLimits.maxRounds(teams.size());
        if (rounds > maxRounds) {
            throw new ApiException(ErrorCode.TOO_MANY_ROUNDS, Map.of("maxRounds", maxRounds));
        }

        deleteAll(melee);
        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        List<ScheduledRound> schedule = new ScheduleGenerator(strategyFor(melee))
                .generate(teamIds, rounds, melee.getSettings().courtCount(), random);
        for (ScheduledRound round : schedule) {
            for (ScheduledMatch match : round.matches()) {
                matchupRepository.save(new Matchup(melee.getId(), round.number(), match.pairing(), match.court()));
            }
            if (round.byeTeam() != null) {
                byeRepository.save(new Bye(melee.getId(), round.number(), round.byeTeam()));
            }
        }
        melee.moveTo(MeleeStatus.MATCHES, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /**
     * With more matches per round than courts, the ones left over are played off court. That is
     * possible, but the admin must know it first: the first attempt is refused with how many.
     */
    @Transactional(readOnly = true)
    public void requireOffCourtAccepted(Melee melee, boolean accepted) {
        int courts = melee.getSettings().courtCount();
        int offCourt = ScheduleLimits.offCourtMatches(teamService.teamsOf(melee.getId()).size(), courts);
        if (offCourt > 0 && !accepted) {
            throw new ApiException(ErrorCode.OFF_COURT_MATCHES, Map.of("offCourtMatches", offCourt, "courts", courts));
        }
    }

    /** Back in the teams and forward again, keeping the schedule and its results. */
    @Transactional
    public void resume(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.TEAMS);
        if (!matchupRepository.existsByMeleeId(melee.getId())) {
            throw new ApiException(ErrorCode.NO_SCHEDULE);
        }
        melee.moveTo(MeleeStatus.MATCHES, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /**
     * One tap marks the X of the winner and the O of the loser. Setting the same winner again does
     * nothing new (a retry from the offline queue is harmless); null clears a mistaken result.
     */
    @Transactional
    public void recordWinner(long meleeId, long clubId, long matchupId, Long winnerTeamId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.MATCHES);
        findInMelee(melee, matchupId).decide(winnerTeamId, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /**
     * An off-court match can move to a court once one is free. The court must not hold another unfinished match
     * of the same round. Courts are also movable if one becomes unusable.
     */
    @Transactional
    public void assignCourt(long meleeId, long clubId, long matchupId, int courtNumber) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.MATCHES);
        if (courtNumber < 1 || courtNumber > melee.getSettings().courtCount()) {
            throw ApiException.validation("courtNumber", "Range");
        }
        Matchup matchup = findInMelee(melee, matchupId);
        if (matchup.isDecided()) {
            throw new ApiException(ErrorCode.INVALID_STATE, Map.of("status", "DECIDED"));
        }
        boolean occupied = scheduleOf(melee.getId()).matchups().stream()
                .anyMatch(other -> !other.getId().equals(matchup.getId())
                        && other.getRoundNumber() == matchup.getRoundNumber()
                        && Integer.valueOf(courtNumber).equals(other.getCourtNumber())
                        && !other.isDecided());
        if (occupied) {
            throw new ApiException(ErrorCode.COURT_OCCUPIED, Map.of("courtNumber", courtNumber));
        }
        matchup.moveToCourt(courtNumber);
        meleeAccess.recordChange(melee);
    }

    /** Deletes through the entities and flushes, so new rounds can reuse the same numbers right after. */
    @Transactional
    public void deleteAll(Melee melee) {
        Schedule schedule = scheduleOf(melee.getId());
        matchupRepository.deleteAll(schedule.matchups());
        byeRepository.deleteAll(schedule.byes());
        matchupRepository.flush();
    }

    @Transactional(readOnly = true)
    public long decidedResults(long meleeId) {
        return matchupRepository.countByMeleeIdAndWinnerTeamIdIsNotNull(meleeId);
    }

    private Matchup findInMelee(Melee melee, long matchupId) {
        return matchupRepository.findByIdAndMeleeId(matchupId, melee.getId()).orElseThrow(ApiException::notFound);
    }

    /** One strategy per melee format; only the classic one exists for now. */
    private PairingStrategy strategyFor(Melee melee) {
        return switch (melee.getFormat()) {
            case CLASSIC -> randomPairing;
        };
    }
}
