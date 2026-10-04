package es.arrima.api.view;

import es.arrima.club.Club;
import es.arrima.club.ClubService;
import es.arrima.club.ScoringTableDto;
import es.arrima.files.FileLinkSigner;
import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeRepository;
import es.arrima.participant.Participant;
import es.arrima.participant.ParticipantRepository;
import es.arrima.participant.ParticipantService;
import es.arrima.schedule.Bye;
import es.arrima.schedule.Matchup;
import es.arrima.schedule.Schedule;
import es.arrima.schedule.ScheduleService;
import es.arrima.schedule.domain.ScheduleLimits;
import es.arrima.schedule.domain.TeamRecord;
import es.arrima.team.Team;
import es.arrima.team.TeamIssues;
import es.arrima.team.TeamService;
import es.arrima.team.domain.TeamPlan;
import es.arrima.team.domain.TeamSizePlanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Builds the read model of a melee from every feature that contributes to it. */
@Component
public class MeleeViewAssembler {

    private final MeleeAccess meleeAccess;
    private final MeleeRepository meleeRepository;
    private final ClubService clubService;
    private final ParticipantService participantService;
    private final ParticipantRepository participantRepository;
    private final TeamService teamService;
    private final ScheduleService scheduleService;
    private final FileLinkSigner fileLinkSigner;

    public MeleeViewAssembler(MeleeAccess meleeAccess, MeleeRepository meleeRepository, ClubService clubService,
            ParticipantService participantService, ParticipantRepository participantRepository,
            TeamService teamService, ScheduleService scheduleService, FileLinkSigner fileLinkSigner) {
        this.meleeAccess = meleeAccess;
        this.meleeRepository = meleeRepository;
        this.clubService = clubService;
        this.participantService = participantService;
        this.participantRepository = participantRepository;
        this.teamService = teamService;
        this.scheduleService = scheduleService;
        this.fileLinkSigner = fileLinkSigner;
    }

    @Transactional(readOnly = true)
    public MeleeView forAdmin(long meleeId, long clubId) {
        return assemble(meleeAccess.forClub(meleeId, clubId));
    }

    @Transactional(readOnly = true)
    public MeleeView assemble(Melee melee) {
        Club club = clubService.getClub(melee.getClubId());
        List<Participant> participants = participantService.listForMelee(melee.getId());
        int activePlayers = (int) participants.stream().filter(Participant::isActive).count();
        List<Team> teams = teamService.teamsOf(melee.getId());
        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        Schedule schedule = scheduleService.scheduleOf(melee.getId());
        int rounds = melee.getSettings().roundsCount();

        return new MeleeView(
                melee.getId(),
                melee.getPublicCode(),
                melee.getPlayedOn(),
                melee.getFormat(),
                melee.getTeamSize(),
                melee.getStatus(),
                meleeRepository.findRevision(melee.getId()),
                new MeleeView.Settings(melee.getSettings().courtCount(), melee.getSettings().roundsCount(),
                        melee.getSettings().prizeCount()),
                ScoringTableDto.from(melee.getScoring()),
                new MeleeView.Club(club.getName(), fileLinkSigner.link(club.getLogoPath())),
                participants.stream().map(MeleeViewAssembler::toView).toList(),
                toView(TeamSizePlanner.plan(activePlayers, melee.getTeamSize())),
                teamViews(teams, schedule.records(teamIds)),
                toView(teamService.issues(teams, participants)),
                ScheduleLimits.maxRounds(teams.size()),
                roundViews(schedule, rounds),
                schedule.exists()
                        ? schedule.counter(teamIds, rounds).stream()
                                .map(target -> new MeleeView.WinTarget(target.wins(), target.reached(), target.canReach()))
                                .toList()
                        : List.of());
    }

    private static List<MeleeView.Team> teamViews(List<Team> teams, List<TeamRecord> records) {
        Map<Long, TeamRecord> recordOf = records.stream().collect(Collectors.toMap(TeamRecord::teamId, Function.identity()));
        return teams.stream().map(team -> {
            TeamRecord record = recordOf.get(team.getId());
            return new MeleeView.Team(team.getId(), team.getNumber(), team.getMemberIds().stream().sorted().toList(),
                    record.wins(), record.losses(), record.pending());
        }).toList();
    }

    private static List<MeleeView.Round> roundViews(Schedule schedule, int rounds) {
        if (!schedule.exists()) {
            return List.of();
        }
        List<MeleeView.Round> views = new ArrayList<>();
        for (int round = 1; round <= rounds; round++) {
            int number = round;
            List<MeleeView.Match> matches = schedule.matchups().stream()
                    .filter(matchup -> matchup.getRoundNumber() == number)
                    .map(MeleeViewAssembler::toView)
                    .toList();
            Long byeTeam = schedule.byes().stream().filter(bye -> bye.getRoundNumber() == number)
                    .map(Bye::getTeamId).findFirst().orElse(null);
            views.add(new MeleeView.Round(number, matches, byeTeam));
        }
        return views;
    }

    private static MeleeView.Match toView(Matchup matchup) {
        return new MeleeView.Match(matchup.getId(), matchup.getTeamAId(), matchup.getTeamBId(),
                matchup.getCourtNumber(), matchup.getWinnerTeamId());
    }

    private static MeleeView.TeamIssues toView(TeamIssues issues) {
        return new MeleeView.TeamIssues(issues.unassignedPlayers(), issues.withdrawnMembers(),
                issues.teamsWithoutActivePlayers());
    }

    @Transactional(readOnly = true)
    public List<MeleeSummary> summaries(List<Melee> melees) {
        Map<Long, Long> activePlayers = participantRepository
                .countActiveByMeleeIds(melees.stream().map(Melee::getId).toList()).stream()
                .collect(Collectors.toMap(ParticipantRepository.ActiveCount::getMeleeId,
                        ParticipantRepository.ActiveCount::getPlayers));
        return melees.stream()
                .map(melee -> new MeleeSummary(melee.getId(), melee.getPlayedOn(), melee.getStatus(),
                        melee.getTeamSize(), melee.getPublicCode(), activePlayers.getOrDefault(melee.getId(), 0L)))
                .toList();
    }

    private static MeleeView.Participant toView(Participant participant) {
        return new MeleeView.Participant(participant.getId(), participant.getListNumber(),
                participant.getDisplayName(), participant.getStatus());
    }

    private static MeleeView.TeamPlan toView(TeamPlan plan) {
        Map<Integer, Long> bySize = plan.teamSizes().stream()
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
        return new MeleeView.TeamPlan(plan.players(), plan.fits(), plan.isPlayable(), plan.teamCount(), bySize);
    }
}
