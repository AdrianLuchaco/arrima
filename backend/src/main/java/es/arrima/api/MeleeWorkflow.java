package es.arrima.api;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeStatus;
import es.arrima.schedule.ScheduleService;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.team.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations that span several features of a melee. Each feature service only knows its own data;
 * here we decide what else must go when something earlier changes, and we ask before losing work:
 * without {@code confirmLosses} the answer is CONFIRMATION_REQUIRED with what would be lost.
 */
@Service
public class MeleeWorkflow {

    private final MeleeAccess meleeAccess;
    private final TeamService teamService;
    private final ScheduleService scheduleService;

    public MeleeWorkflow(MeleeAccess meleeAccess, TeamService teamService, ScheduleService scheduleService) {
        this.meleeAccess = meleeAccess;
        this.teamService = teamService;
        this.scheduleService = scheduleService;
    }

    /** New teams invalidate the schedule built on the old ones and everything after it. */
    @Transactional
    public void drawTeams(long meleeId, long clubId, boolean acceptDifferentTeam, boolean confirmLosses) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.REGISTRATION, MeleeStatus.TEAMS);
        requireConfirmationIfLosing(lossesFromScheduleOn(melee), confirmLosses);
        scheduleService.deleteAll(melee);
        teamService.draw(melee, acceptDifferentTeam);
    }

    /** A new schedule throws away the results of the previous one. */
    @Transactional
    public void generateSchedule(long meleeId, long clubId, boolean confirmLosses) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.TEAMS);
        requireConfirmationIfLosing(lossesFromScheduleOn(melee), confirmLosses);
        scheduleService.generate(melee);
    }

    private Losses lossesFromScheduleOn(Melee melee) {
        return new Losses(scheduleService.decidedResults(melee.getId()), 0, 0);
    }

    private static void requireConfirmationIfLosing(Losses losses, boolean confirmed) {
        if (losses.any() && !confirmed) {
            throw new ApiException(ErrorCode.CONFIRMATION_REQUIRED, losses.asDetails());
        }
    }
}
