package es.arrima.api;

import es.arrima.api.MeleeRequests.RecordThrow;
import es.arrima.api.MeleeRequests.StartInternational;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.international.InternationalService;
import es.arrima.international.domain.ThrowKind;
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
@RequestMapping("/api/melees/{meleeId}/international")
class InternationalController {

    private final MeleeWorkflow workflow;
    private final InternationalService internationalService;
    private final MeleeViewAssembler views;

    InternationalController(MeleeWorkflow workflow, InternationalService internationalService, MeleeViewAssembler views) {
        this.workflow = workflow;
        this.internationalService = internationalService;
        this.views = views;
    }

    @PostMapping("/start")
    MeleeView start(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @RequestBody StartInternational request) {
        workflow.startInternational(meleeId, admin.clubId(), request.confirmsLosses());
        return views.forAdmin(meleeId, admin.clubId());
    }

    /**
     * One ball, identified by its place (round, team, kind, number): PUT, so recording it again (from
     * the offline queue) or correcting it later uses the same request.
     */
    @PutMapping("/rounds/{roundId}/teams/{teamId}/throws/{kind}/{ballNumber}")
    MeleeView recordThrow(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long roundId, @PathVariable long teamId, @PathVariable ThrowKind kind,
            @PathVariable int ballNumber, @Valid @RequestBody RecordThrow request) {
        internationalService.recordThrow(meleeId, admin.clubId(), roundId, teamId, kind, ballNumber, request.outcome());
        return views.forAdmin(meleeId, admin.clubId());
    }
}
