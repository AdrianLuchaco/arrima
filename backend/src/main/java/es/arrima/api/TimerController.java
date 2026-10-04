package es.arrima.api;

import es.arrima.api.MeleeRequests.StartTimer;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.melee.MeleeAccess;
import es.arrima.shared.security.AdminPrincipal;
import es.arrima.timer.TimerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The countdown of each round: start, pause, resume, cancel. */
@RestController
@RequestMapping("/api/melees/{meleeId}/timer")
class TimerController {

    private final TimerService timerService;
    private final MeleeAccess meleeAccess;
    private final MeleeViewAssembler views;

    TimerController(TimerService timerService, MeleeAccess meleeAccess, MeleeViewAssembler views) {
        this.timerService = timerService;
        this.meleeAccess = meleeAccess;
        this.views = views;
    }

    @PostMapping("/rounds/{roundNumber}/start")
    MeleeView start(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable int roundNumber, @RequestBody StartTimer request) {
        timerService.start(meleeId, admin.clubId(), roundNumber, request.stopsRunningCountdown());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/rounds/{roundNumber}/pause")
    MeleeView pause(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable int roundNumber) {
        timerService.pause(meleeId, admin.clubId(), roundNumber);
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/rounds/{roundNumber}/resume")
    MeleeView resume(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable int roundNumber) {
        timerService.resume(meleeId, admin.clubId(), roundNumber);
        return views.forAdmin(meleeId, admin.clubId());
    }

    @DeleteMapping("/rounds/{roundNumber}")
    MeleeView cancel(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable int roundNumber) {
        timerService.cancel(meleeId, admin.clubId(), roundNumber);
        return views.forAdmin(meleeId, admin.clubId());
    }

    /**
     * The admin's phone calls this when its countdown reaches 0: one more trigger to record the end
     * (see TimerTriggers). The server decides with its own clock; it never ends a countdown early.
     */
    @PostMapping("/check")
    MeleeView check(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        timerService.expireIfDue(meleeAccess.forClub(meleeId, admin.clubId()).getId());
        return views.forAdmin(meleeId, admin.clubId());
    }
}
