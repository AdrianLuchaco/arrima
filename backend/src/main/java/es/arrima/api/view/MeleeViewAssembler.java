package es.arrima.api.view;

import es.arrima.club.Club;
import es.arrima.club.ClubService;
import es.arrima.club.ScoringTableDto;
import es.arrima.files.FileLinkSigner;
import es.arrima.international.InternationalService;
import es.arrima.prize.Prize;
import es.arrima.prize.PrizePhoto;
import es.arrima.prize.PrizeService;
import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeRepository;
import es.arrima.melee.MeleeStatus;
import es.arrima.participant.Participant;
import es.arrima.participant.ParticipantRepository;
import es.arrima.participant.ParticipantService;
import es.arrima.participant.PaymentSummary;
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
    private final InternationalService internationalService;
    private final PrizeService prizeService;
    private final FileLinkSigner fileLinkSigner;

    public MeleeViewAssembler(MeleeAccess meleeAccess, MeleeRepository meleeRepository, ClubService clubService,
            ParticipantService participantService, ParticipantRepository participantRepository,
            TeamService teamService, ScheduleService scheduleService, InternationalService internationalService,
            PrizeService prizeService, FileLinkSigner fileLinkSigner) {
        this.meleeAccess = meleeAccess;
        this.meleeRepository = meleeRepository;
        this.clubService = clubService;
        this.participantService = participantService;
        this.participantRepository = participantRepository;
        this.teamService = teamService;
        this.scheduleService = scheduleService;
        this.internationalService = internationalService;
        this.prizeService = prizeService;
        this.fileLinkSigner = fileLinkSigner;
    }

    @Transactional(readOnly = true)
    public MeleeView forAdmin(long meleeId, long clubId) {
        return assemble(meleeAccess.forClub(meleeId, clubId), Audience.ADMIN);
    }

    @Transactional(readOnly = true)
    public MeleeView assemble(Melee melee, Audience audience) {
        Club club = clubService.getClub(melee.getClubId());
        List<Participant> participants = participantService.listForMelee(melee.getId());
        boolean paymentRequired = melee.requiresPayment();
        boolean showsPayments = audience == Audience.ADMIN && paymentRequired;
        boolean afterDraw = melee.getStatus() != MeleeStatus.REGISTRATION;
        // The team plan counts who will play. While payments are being collected, spectators get it
        // counted without payments: otherwise the number would tell them how many have paid.
        boolean planCountsPayments = paymentRequired && (audience == Audience.ADMIN || afterDraw);
        int players = (int) participants.stream()
                .filter(participant -> participant.plays(planCountsPayments))
                .count();
        List<Team> teams = shows(audience, melee, MeleeStatus.TEAMS) ? teamService.teamsOf(melee.getId()) : List.of();
        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        Schedule schedule = shows(audience, melee, MeleeStatus.MATCHES)
                ? scheduleService.scheduleOf(melee.getId())
                : new Schedule(List.of(), List.of());
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
                        melee.getSettings().prizeCount(), melee.getSettings().entryFeeCents()),
                ScoringTableDto.from(melee.getScoring()),
                new MeleeView.Club(club.getName(), fileLinkSigner.link(club.getLogoPath())),
                participants.stream()
                        .map(participant -> toView(participant, showsPayments,
                                afterDraw && participant.isActive() && !participant.plays(paymentRequired)))
                        .toList(),
                toView(TeamSizePlanner.plan(players, melee.getTeamSize())),
                teamViews(teams, schedule.records(teamIds)),
                toView(teamService.issues(melee, teams, participants)),
                ScheduleLimits.maxRounds(teams.size()),
                roundViews(schedule, rounds),
                schedule.exists()
                        ? schedule.counter(teamIds, rounds).stream()
                                .map(target -> new MeleeView.WinTarget(target.wins(), target.reached(), target.canReach()))
                                .toList()
                        : List.of(),
                internationalView(melee, audience, teams),
                prizeViews(melee, audience),
                showsPayments ? toView(PaymentSummary.of(participants, melee.getSettings().entryFeeCents())) : null);
    }

    /**
     * The admin sees every prize. Spectators see them from the ceremony on, one by one as they are
     * handed out, and all of them once the melee is closed.
     */
    private List<MeleeView.PrizeView> prizeViews(Melee melee, Audience audience) {
        if (!shows(audience, melee, MeleeStatus.PRIZES)) {
            return List.of();
        }
        boolean onlyAwarded = audience == Audience.PUBLIC && melee.getStatus() == MeleeStatus.PRIZES;
        List<Prize> prizes = prizeService.prizesOf(melee.getId()).stream()
                .filter(prize -> !onlyAwarded || prize.isAwarded())
                .toList();
        Map<Long, List<PrizePhoto>> photos = prizeService.photosOf(prizes);
        return prizes.stream()
                .map(prize -> new MeleeView.PrizeView(prize.getId(), prize.getPosition(), prize.getTeamId(),
                        prize.getInternationalPoints(), prize.isAwarded(),
                        photos.getOrDefault(prize.getId(), List.of()).stream()
                                .map(photo -> new MeleeView.Photo(photo.getId(), fileLinkSigner.link(photo.getStoragePath())))
                                .toList()))
                .toList();
    }

    /** Shown from the moment it starts; the admin also sees it while back in an earlier phase. */
    private MeleeView.International internationalView(Melee melee, Audience audience, List<Team> teams) {
        boolean reached = melee.getStatus().isAtLeast(MeleeStatus.INTERNATIONAL);
        if (!reached && (audience == Audience.PUBLIC || internationalService.ballThrowCount(melee.getId()) == 0)) {
            return null;
        }
        Map<Long, Integer> teamSizes = teams.stream().collect(Collectors.toMap(Team::getId, team -> team.getMemberIds().size()));
        return InternationalViews.of(internationalService.stateOf(melee), teamSizes);
    }

    /** Spectators only see the data of phases the melee has reached (see Audience). */
    private static boolean shows(Audience audience, Melee melee, MeleeStatus phase) {
        return audience == Audience.ADMIN || melee.getStatus().isAtLeast(phase);
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
        return new MeleeView.TeamIssues(issues.unassignedPlayers(), issues.membersNotPlaying(),
                issues.teamsWithoutPlayers());
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

    private static MeleeView.Participant toView(Participant participant, boolean showsPayment, boolean notPlaying) {
        return new MeleeView.Participant(participant.getId(), participant.getListNumber(),
                participant.getDisplayName(), participant.getStatus(),
                showsPayment ? participant.getPaymentStatus() : null, notPlaying);
    }

    private static MeleeView.Payments toView(PaymentSummary summary) {
        return new MeleeView.Payments(summary.expected(), summary.paid(), summary.unpaid(), summary.unmarked(),
                summary.entryFeeCents(), summary.collectedCents());
    }

    private static MeleeView.TeamPlan toView(TeamPlan plan) {
        Map<Integer, Long> bySize = plan.teamSizes().stream()
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
        return new MeleeView.TeamPlan(plan.players(), plan.fits(), plan.isPlayable(), plan.teamCount(), bySize);
    }
}
