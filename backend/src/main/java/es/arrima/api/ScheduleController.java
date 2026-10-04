package es.arrima.api;

import es.arrima.api.MeleeRequests.AssignCourt;
import es.arrima.api.MeleeRequests.GenerateSchedule;
import es.arrima.api.MeleeRequests.SetWinner;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.schedule.ScheduleService;
import es.arrima.shared.security.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/melees/{meleeId}/schedule")
class ScheduleController {

    private final MeleeWorkflow workflow;
    private final ScheduleService scheduleService;
    private final MeleeViewAssembler views;

    ScheduleController(MeleeWorkflow workflow, ScheduleService scheduleService, MeleeViewAssembler views) {
        this.workflow = workflow;
        this.scheduleService = scheduleService;
        this.views = views;
    }

    @PostMapping("/generate")
    MeleeView generate(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @RequestBody GenerateSchedule request) {
        workflow.generateSchedule(meleeId, admin.clubId(), request.confirmsLosses());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/resume")
    MeleeView resume(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        scheduleService.resume(meleeId, admin.clubId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    /** PUT: idempotent, so the offline queue can safely send it again. */
    @PutMapping("/matchups/{matchupId}/winner")
    MeleeView setWinner(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long matchupId, @RequestBody SetWinner request) {
        scheduleService.recordWinner(meleeId, admin.clubId(), matchupId, request.winnerTeamId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PutMapping("/matchups/{matchupId}/court")
    MeleeView assignCourt(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long matchupId, @Valid @RequestBody AssignCourt request) {
        scheduleService.assignCourt(meleeId, admin.clubId(), matchupId, request.courtNumber());
        return views.forAdmin(meleeId, admin.clubId());
    }
}
