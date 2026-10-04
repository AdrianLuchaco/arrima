package es.arrima.team;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeStatus;
import es.arrima.participant.Participant;
import es.arrima.participant.ParticipantService;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.team.domain.RandomTeamFormation;
import es.arrima.team.domain.TeamFormationStrategy;
import es.arrima.team.domain.TeamPlan;
import es.arrima.team.domain.TeamSizePlanner;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final ParticipantService participantService;
    private final MeleeAccess meleeAccess;
    private final Clock clock;
    /** Unpredictable: nobody can work out or influence the draw. */
    private final RandomGenerator random = new SecureRandom();
    private final TeamFormationStrategy randomFormation = new RandomTeamFormation();

    public TeamService(TeamRepository teamRepository, ParticipantService participantService, MeleeAccess meleeAccess,
            Clock clock) {
        this.teamRepository = teamRepository;
        this.participantService = participantService;
        this.meleeAccess = meleeAccess;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Team> teamsOf(long meleeId) {
        return teamRepository.findByMeleeIdWithMembers(meleeId);
    }

    /**
     * "Generar equipos": draws the players (see Participant#plays) into new teams, replacing any
     * previous ones.
     * When the players do not fit, the "different team" (e.g. one triplette among doublettes) is only
     * used if the admin chose it; otherwise the screen offers the other solutions first.
     * The caller (MeleeWorkflow) has already dealt with what depended on the old teams.
     */
    @Transactional
    public void draw(Melee melee, boolean acceptDifferentTeam) {
        melee.requireStatus(MeleeStatus.REGISTRATION, MeleeStatus.TEAMS);
        List<Long> players = participantService.listForMelee(melee.getId()).stream()
                .filter(participant -> participant.plays(melee.requiresPayment()))
                .map(Participant::getId).toList();
        TeamPlan plan = TeamSizePlanner.plan(players.size(), melee.getTeamSize());
        if (!plan.isPlayable()) {
            throw new ApiException(ErrorCode.NOT_ENOUGH_PLAYERS, Map.of("players", players.size()));
        }
        if (!plan.fits() && !acceptDifferentTeam) {
            throw new ApiException(ErrorCode.TEAMS_DO_NOT_FIT, Map.of("players", players.size()));
        }

        deleteAll(melee);
        List<List<Long>> drawn = strategyFor(melee).formTeams(players, plan, random);
        for (int i = 0; i < drawn.size(); i++) {
            teamRepository.save(new Team(melee.getId(), i + 1, new HashSet<>(drawn.get(i))));
        }
        melee.moveTo(MeleeStatus.TEAMS, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /** Back in the sign-up list and forward again, keeping the teams already drawn. */
    @Transactional
    public void resume(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.REGISTRATION);
        if (!teamRepository.existsByMeleeId(melee.getId())) {
            throw new ApiException(ErrorCode.NO_TEAMS);
        }
        melee.moveTo(MeleeStatus.TEAMS, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /** Before the matches start, the admin can exchange two players of different teams. */
    @Transactional
    public void swapPlayers(long meleeId, long clubId, long firstPlayerId, long secondPlayerId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.TEAMS);
        List<Team> teams = teamsOf(melee.getId());
        Team first = teamOf(teams, firstPlayerId);
        Team second = teamOf(teams, secondPlayerId);
        if (first.getId().equals(second.getId())) {
            throw ApiException.validation("secondPlayerId", "SameTeam");
        }
        first.replaceMember(firstPlayerId, secondPlayerId);
        second.replaceMember(secondPlayerId, firstPlayerId);
        meleeAccess.recordChange(melee);
    }

    /**
     * Someone leaves after the draw (withdrawn, or recorded as not paid): another player who is in
     * no team takes their place. Allowed during the matches too: the team, its number and results stay.
     */
    @Transactional
    public void substitute(long meleeId, long clubId, long leavingPlayerId, long joiningPlayerId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.TEAMS, MeleeStatus.MATCHES);
        List<Team> teams = teamsOf(melee.getId());
        Team team = teamOf(teams, leavingPlayerId);
        Participant joining = participantService.findInMelee(melee, joiningPlayerId);
        boolean alreadyInATeam = teams.stream().anyMatch(candidate -> candidate.hasMember(joiningPlayerId));
        if (!joining.plays(melee.requiresPayment()) || alreadyInATeam) {
            throw ApiException.validation("joiningPlayerId", "NotAvailable");
        }
        team.replaceMember(leavingPlayerId, joiningPlayerId);
        meleeAccess.recordChange(melee);
    }

    @Transactional(readOnly = true)
    public boolean isInATeam(Melee melee, long participantId) {
        return teamsOf(melee.getId()).stream().anyMatch(team -> team.hasMember(participantId));
    }

    /**
     * Deletes through the entities (not a bulk query) so Hibernate's session stays consistent, and
     * flushes right away: Hibernate runs inserts before deletes, and new teams reuse the numbers.
     */
    @Transactional
    public void deleteAll(Melee melee) {
        teamRepository.deleteAll(teamsOf(melee.getId()));
        teamRepository.flush();
    }

    /** What no longer adds up between the sign-up list and the teams (see TeamIssues). */
    public TeamIssues issues(Melee melee, List<Team> teams, List<Participant> participants) {
        if (teams.isEmpty()) {
            return TeamIssues.NONE;
        }
        boolean paymentRequired = melee.requiresPayment();
        Set<Long> inTeams = teams.stream().flatMap(team -> team.getMemberIds().stream()).collect(Collectors.toSet());
        Set<Long> players = participants.stream().filter(participant -> participant.plays(paymentRequired))
                .map(Participant::getId).collect(Collectors.toSet());
        List<Long> unassigned = participants.stream()
                .filter(participant -> players.contains(participant.getId()) && !inTeams.contains(participant.getId()))
                .map(Participant::getId).toList();
        List<Long> membersNotPlaying = participants.stream()
                .filter(participant -> !players.contains(participant.getId()) && inTeams.contains(participant.getId()))
                .map(Participant::getId).toList();
        List<Integer> empty = teams.stream()
                .filter(team -> team.getMemberIds().stream().noneMatch(players::contains))
                .map(Team::getNumber).toList();
        return new TeamIssues(unassigned, membersNotPlaying, empty);
    }

    private static Team teamOf(List<Team> teams, long playerId) {
        return teams.stream().filter(team -> team.hasMember(playerId)).findFirst().orElseThrow(ApiException::notFound);
    }

    /** One strategy per melee format; only the classic one exists for now. */
    private TeamFormationStrategy strategyFor(Melee melee) {
        return switch (melee.getFormat()) {
            case CLASSIC -> randomFormation;
        };
    }
}
