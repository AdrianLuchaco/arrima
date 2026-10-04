package es.arrima.international;

import es.arrima.club.ScoringTable;
import es.arrima.international.InternationalGroup.GroupStatus;
import es.arrima.international.InternationalState.CurrentTurn;
import es.arrima.international.InternationalState.GroupState;
import es.arrima.international.InternationalState.RoundState;
import es.arrima.international.domain.FinalRanking;
import es.arrima.international.domain.FinalRanking.RankedTeam;
import es.arrima.international.domain.GroupRanking;
import es.arrima.international.domain.GroupRanking.GroupStanding;
import es.arrima.international.domain.GroupRanking.RoundResult;
import es.arrima.international.domain.InternationalPlanner;
import es.arrima.international.domain.PlannedGroup;
import es.arrima.international.domain.PointsTable;
import es.arrima.international.domain.ThrowKind;
import es.arrima.international.domain.ThrowOutcome;
import es.arrima.international.domain.ThrowSequence;
import es.arrima.international.domain.TeamWins;
import es.arrima.international.domain.TurnOrder;
import es.arrima.international.domain.TurnOrder.ThrowKey;
import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeStatus;
import es.arrima.schedule.Schedule;
import es.arrima.schedule.ScheduleService;
import es.arrima.schedule.domain.TeamRecord;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.team.Team;
import es.arrima.team.TeamService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternationalService {

    private static final int BALLS_PER_TEAM = 2 * ThrowSequence.BALLS_PER_KIND;

    private final InternationalGroupRepository groupRepository;
    private final InternationalRoundRepository roundRepository;
    private final BallThrowRepository throwRepository;
    private final TeamService teamService;
    private final ScheduleService scheduleService;
    private final MeleeAccess meleeAccess;
    private final Clock clock;

    public InternationalService(InternationalGroupRepository groupRepository, InternationalRoundRepository roundRepository,
            BallThrowRepository throwRepository, TeamService teamService, ScheduleService scheduleService,
            MeleeAccess meleeAccess, Clock clock) {
        this.groupRepository = groupRepository;
        this.roundRepository = roundRepository;
        this.throwRepository = throwRepository;
        this.teamService = teamService;
        this.scheduleService = scheduleService;
        this.meleeAccess = meleeAccess;
        this.clock = clock;
    }

    // ----------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public InternationalState stateOf(Melee melee) {
        List<Team> teams = teamService.teamsOf(melee.getId());
        List<PlannedGroup> plan = planFor(melee, teams);
        Map<Long, Integer> teamSizes = teams.stream()
                .collect(Collectors.toMap(Team::getId, team -> team.getMemberIds().size()));

        List<GroupState> groups = loadGroups(melee.getId());
        CurrentTurn turn = currentTurn(groups, teamSizes);
        Map<PlannedGroup, GroupStanding> standings = standingsOfPlan(plan, groups);
        boolean complete = groups.stream().allMatch(group -> group.standing().complete())
                && standings.size() == InternationalPlanner.playingOrder(plan).size();
        List<RankedTeam> finalRanking = complete ? FinalRanking.of(plan, standings) : List.of();
        return new InternationalState(plan, groups, turn, finalRanking, complete);
    }

    /** The winners for the prize ceremony, with the points shown next to each name. */
    @Transactional(readOnly = true)
    public List<PrizeWinner> prizeWinners(Melee melee) {
        InternationalState state = stateOf(melee);
        if (!state.complete()) {
            throw new ApiException(ErrorCode.INTERNATIONAL_INCOMPLETE);
        }
        Map<Long, Integer> mainRoundPoints = new HashMap<>();
        state.groups().forEach(group -> group.rounds().stream()
                .filter(round -> round.round().getRoundNumber() == 1)
                .forEach(round -> round.round().getTeams()
                        .forEach(team -> mainRoundPoints.put(team.teamId(), round.pointsOf(team.teamId())))));
        return state.finalRanking().stream()
                .map(ranked -> new PrizeWinner(ranked.position(), ranked.teamId(), mainRoundPoints.get(ranked.teamId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public long ballThrowCount(long meleeId) {
        return throwRepository.countByMeleeId(meleeId);
    }

    /** Balls that starting (again) would delete: those of groups whose teams are no longer the same. */
    @Transactional(readOnly = true)
    public long ballThrowsLostIfStarted(Melee melee) {
        Set<Set<Long>> plannedTeamSets = playingTeamSets(melee);
        return loadGroups(melee.getId()).stream()
                .filter(group -> !plannedTeamSets.contains(Set.copyOf(group.teamIds())))
                .mapToLong(group -> group.rounds().stream().mapToLong(round -> round.ballThrows().size()).sum())
                .sum();
    }

    // ----------------------------------------------------------------- changing

    /**
     * "Iniciar la Internacional", once every match has its result. If it had been played before
     * (going back to fix a result), groups with the same teams keep their balls; the rest are
     * replaced. MeleeWorkflow asks for confirmation before balls are lost.
     */
    @Transactional
    public void start(Melee melee) {
        melee.requireStatus(MeleeStatus.MATCHES);
        long pendingResults = scheduleService.scheduleOf(melee.getId()).pendingResults();
        if (pendingResults > 0) {
            throw new ApiException(ErrorCode.RESULTS_MISSING, Map.of("pending", pendingResults));
        }
        List<Team> teams = teamService.teamsOf(melee.getId());
        List<PlannedGroup> playing = InternationalPlanner.playingOrder(planFor(melee, teams));
        Map<Set<Long>, GroupState> existing = loadGroups(melee.getId()).stream()
                .collect(Collectors.toMap(group -> Set.copyOf(group.teamIds()), Function.identity()));

        List<InternationalGroup> groups = new ArrayList<>();
        for (int i = 0; i < playing.size(); i++) {
            PlannedGroup planned = playing.get(i);
            GroupState same = existing.remove(Set.copyOf(planned.teamIds()));
            if (same != null) {
                same.group().update(planned, i + 1);
                groups.add(same.group());
            } else {
                InternationalGroup created = groupRepository.save(new InternationalGroup(melee.getId(), planned, i + 1));
                // Round 1: every team of the group, in team-number order (the planner's order).
                roundRepository.save(new InternationalRound(created.getId(), 1, planned.teamIds()));
                groups.add(created);
            }
        }
        existing.values().forEach(this::deleteGroup);
        groupRepository.flush();

        Map<Long, Integer> teamNumbers = teams.stream().collect(Collectors.toMap(Team::getId, Team::getNumber));
        groups.forEach(group -> syncRounds(group, teamNumbers));
        melee.moveTo(MeleeStatus.INTERNATIONAL, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /**
     * Records (or corrects) one ball. The same ball sent twice is stored once: the offline queue can
     * repeat it safely. After each ball the group decides whether a tie-break round is needed.
     */
    @Transactional
    public void recordThrow(long meleeId, long clubId, long roundId, long teamId, ThrowKind kind, int ballNumber,
            ThrowOutcome outcome) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.INTERNATIONAL);
        InternationalRound round = roundRepository.findById(roundId).orElseThrow(ApiException::notFound);
        InternationalGroup group = groupRepository.findById(round.getGroupId())
                .filter(candidate -> candidate.getMeleeId() == melee.getId())
                .orElseThrow(ApiException::notFound);
        if (!round.hasTeam(teamId)) {
            throw ApiException.notFound();
        }
        if (outcome.kind() != kind) {
            throw ApiException.validation("outcome", "WrongKind");
        }
        if (ballNumber < 1 || ballNumber > ThrowSequence.BALLS_PER_KIND) {
            throw ApiException.validation("ballNumber", "Range");
        }

        int points = pointsTable(melee.getScoring()).pointsFor(outcome);
        Optional<BallThrow> existing = throwRepository.findByRoundIdAndTeamIdAndKindAndBallNumber(roundId, teamId, kind, ballNumber);
        if (existing.isPresent()) {
            existing.get().correct(outcome, points, clock.instant());
        } else {
            throwRepository.save(new BallThrow(roundId, teamId, kind, ballNumber, outcome, points, clock.instant()));
        }
        throwRepository.flush();

        Map<Long, Integer> teamNumbers = teamService.teamsOf(melee.getId()).stream()
                .collect(Collectors.toMap(Team::getId, Team::getNumber));
        syncRounds(group, teamNumbers);
        meleeAccess.recordChange(melee);
    }

    /** Used when teams or the schedule are redone: everything of la Internacional goes. */
    @Transactional
    public void deleteAll(Melee melee) {
        loadGroups(melee.getId()).forEach(this::deleteGroup);
        groupRepository.flush();
    }

    // ----------------------------------------------------------------- internals

    /**
     * After any change in a group: removes tie-break rounds that a correction made unnecessary and
     * that nobody played yet, creates the round the group needs now (if any) and updates its status.
     */
    private void syncRounds(InternationalGroup group, Map<Long, Integer> teamNumbers) {
        GroupState state = loadGroup(group);
        state.rounds().stream()
                .filter(round -> round.obsolete() && round.ballThrows().isEmpty())
                .forEach(round -> roundRepository.delete(round.round()));

        Set<Long> tie = state.standing().tieToBreak();
        if (!tie.isEmpty()) {
            int nextNumber = state.rounds().stream().mapToInt(round -> round.round().getRoundNumber()).max().orElse(0) + 1;
            List<Long> inOrder = tie.stream().sorted(Comparator.comparing(teamNumbers::get)).toList();
            roundRepository.save(new InternationalRound(group.getId(), nextNumber, inOrder));
        }
        boolean anyBall = state.rounds().stream().anyMatch(round -> !round.ballThrows().isEmpty());
        group.changeStatus(state.standing().complete() ? GroupStatus.FINISHED : anyBall ? GroupStatus.IN_PROGRESS : GroupStatus.PENDING);
        roundRepository.flush();
    }

    private List<GroupState> loadGroups(long meleeId) {
        List<InternationalGroup> groups = groupRepository.findByMeleeIdOrderByPlayOrder(meleeId);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<InternationalRound> rounds = roundRepository.findByGroupIdsWithTeams(groups.stream().map(InternationalGroup::getId).toList());
        Map<Long, List<BallThrow>> throwsByRound = rounds.isEmpty() ? Map.of()
                : throwRepository.findByRoundIdIn(rounds.stream().map(InternationalRound::getId).toList()).stream()
                        .collect(Collectors.groupingBy(BallThrow::getRoundId));
        Map<Long, List<InternationalRound>> roundsByGroup = rounds.stream()
                .collect(Collectors.groupingBy(InternationalRound::getGroupId));
        return groups.stream()
                .map(group -> groupState(group, roundsByGroup.getOrDefault(group.getId(), List.of()), throwsByRound))
                .toList();
    }

    private GroupState loadGroup(InternationalGroup group) {
        List<InternationalRound> rounds = roundRepository.findByGroupIdsWithTeams(List.of(group.getId()));
        Map<Long, List<BallThrow>> throwsByRound = rounds.isEmpty() ? Map.of()
                : throwRepository.findByRoundIdIn(rounds.stream().map(InternationalRound::getId).toList()).stream()
                        .collect(Collectors.groupingBy(BallThrow::getRoundId));
        return groupState(group, rounds, throwsByRound);
    }

    /** The group's teams are those of its first round; the rounds are ranked with the domain rules. */
    private static GroupState groupState(InternationalGroup group, List<InternationalRound> rounds,
            Map<Long, List<BallThrow>> throwsByRound) {
        List<InternationalRound> ordered = rounds.stream().sorted(Comparator.comparingInt(InternationalRound::getRoundNumber)).toList();
        List<Long> teamIds = ordered.isEmpty() ? List.of()
                : ordered.getFirst().getTeams().stream().map(RoundTeam::teamId).toList();

        List<RoundResult> results = ordered.stream().map(round -> {
            List<BallThrow> balls = throwsByRound.getOrDefault(round.getId(), List.of());
            Map<Long, Integer> points = new HashMap<>();
            round.teamIdSet().forEach(team -> points.put(team, 0));
            balls.forEach(ball -> points.merge(ball.getTeamId(), ball.getPoints(), Integer::sum));
            boolean complete = balls.size() == round.getTeams().size() * BALLS_PER_TEAM;
            return new RoundResult(round.getId(), round.teamIdSet(), points, complete);
        }).toList();

        GroupStanding standing = teamIds.isEmpty()
                ? new GroupStanding(List.of(), Set.of(), Set.of(), false)
                : GroupRanking.rank(teamIds, group.getBestPrizePosition(), group.getWorstPrizePosition(), results);

        List<RoundState> roundStates = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            InternationalRound round = ordered.get(i);
            roundStates.add(new RoundState(round, throwsByRound.getOrDefault(round.getId(), List.of()),
                    standing.obsoleteRounds().contains(round.getId()), results.get(i).complete()));
        }
        return new GroupState(group, teamIds, roundStates, standing);
    }

    /** The first ball still missing in the first undecided group, in its first unfinished round. */
    private static CurrentTurn currentTurn(List<GroupState> groups, Map<Long, Integer> teamSizes) {
        for (GroupState group : groups) {
            if (group.standing().complete()) {
                continue;
            }
            for (RoundState round : group.rounds()) {
                if (round.obsolete() || round.complete()) {
                    continue;
                }
                List<TurnOrder.Entry> entries = round.round().getTeams().stream()
                        .map(team -> new TurnOrder.Entry(team.teamId(), teamSizes.getOrDefault(team.teamId(), 2)))
                        .toList();
                Set<ThrowKey> recorded = round.ballThrows().stream()
                        .map(ball -> new ThrowKey(ball.getTeamId(), ball.getKind(), ball.getBallNumber()))
                        .collect(Collectors.toSet());
                Optional<TurnOrder.Turn> turn = TurnOrder.next(entries, recorded);
                if (turn.isPresent()) {
                    return new CurrentTurn(group.group().getId(), round.round().getId(), turn.get().teamId(), turn.get().slot());
                }
            }
            return null; // waiting for the next round to be created (it is, as soon as the server sees the last ball)
        }
        return null;
    }

    private static Map<PlannedGroup, GroupStanding> standingsOfPlan(List<PlannedGroup> plan, List<GroupState> groups) {
        Map<Set<Long>, GroupStanding> byTeams = new HashMap<>();
        groups.forEach(group -> byTeams.put(Set.copyOf(group.teamIds()), group.standing()));
        Map<PlannedGroup, GroupStanding> standings = new HashMap<>();
        for (PlannedGroup planned : InternationalPlanner.playingOrder(plan)) {
            GroupStanding standing = byTeams.get(Set.copyOf(planned.teamIds()));
            if (standing != null) {
                standings.put(planned, standing);
            }
        }
        return standings;
    }

    private List<PlannedGroup> planFor(Melee melee, List<Team> teams) {
        Schedule schedule = scheduleService.scheduleOf(melee.getId());
        Map<Long, Team> teamById = teams.stream().collect(Collectors.toMap(Team::getId, Function.identity()));
        List<TeamWins> wins = schedule.records(teamById.keySet()).stream()
                .map((TeamRecord record) -> new TeamWins(record.teamId(), teamById.get(record.teamId()).getNumber(), record.wins()))
                .toList();
        return InternationalPlanner.plan(wins, melee.getSettings().prizeCount());
    }

    private Set<Set<Long>> playingTeamSets(Melee melee) {
        Set<Set<Long>> sets = new HashSet<>();
        InternationalPlanner.playingOrder(planFor(melee, teamService.teamsOf(melee.getId())))
                .forEach(group -> sets.add(Set.copyOf(group.teamIds())));
        return sets;
    }

    private void deleteGroup(GroupState group) {
        group.rounds().forEach(round -> throwRepository.deleteAll(round.ballThrows()));
        throwRepository.flush();
        group.rounds().forEach(round -> roundRepository.delete(round.round()));
        roundRepository.flush();
        groupRepository.delete(group.group());
    }

    static PointsTable pointsTable(ScoringTable table) {
        Map<ThrowOutcome, Integer> points = new EnumMap<>(ThrowOutcome.class);
        points.put(ThrowOutcome.OUT, table.pointingOut());
        points.put(ThrowOutcome.BIG_CIRCLE, table.pointingBigCircle());
        points.put(ThrowOutcome.SMALL_CIRCLE, table.pointingSmallCircle());
        points.put(ThrowOutcome.NEAR_JACK, table.pointingNearJack());
        points.put(ThrowOutcome.ON_JACK, table.pointingOnJack());
        points.put(ThrowOutcome.MISS, table.shootingMiss());
        points.put(ThrowOutcome.HIT, table.shootingHit());
        points.put(ThrowOutcome.HIT_OUT, table.shootingHitOut());
        points.put(ThrowOutcome.CARREAU, table.shootingCarreau());
        return new PointsTable(points);
    }
}
