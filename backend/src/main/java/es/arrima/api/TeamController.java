package es.arrima.api;

import es.arrima.api.MeleeRequests.DrawTeams;
import es.arrima.api.MeleeRequests.Substitute;
import es.arrima.api.MeleeRequests.SwapPlayers;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.shared.security.AdminPrincipal;
import es.arrima.team.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/melees/{meleeId}/teams")
class TeamController {

    private final MeleeWorkflow workflow;
    private final TeamService teamService;
    private final MeleeViewAssembler views;

    TeamController(MeleeWorkflow workflow, TeamService teamService, MeleeViewAssembler views) {
        this.workflow = workflow;
        this.teamService = teamService;
        this.views = views;
    }

    /** "Generar equipos" (and "repetir el sorteo"). */
    @PostMapping("/draw")
    MeleeView draw(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @RequestBody DrawTeams request) {
        workflow.drawTeams(meleeId, admin.clubId(), request.acceptsDifferentTeam(), request.confirmsLosses());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/resume")
    MeleeView resume(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        teamService.resume(meleeId, admin.clubId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/swap")
    MeleeView swap(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody SwapPlayers request) {
        teamService.swapPlayers(meleeId, admin.clubId(), request.firstPlayerId(), request.secondPlayerId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/substitute")
    MeleeView substitute(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody Substitute request) {
        teamService.substitute(meleeId, admin.clubId(), request.leavingPlayerId(), request.joiningPlayerId());
        return views.forAdmin(meleeId, admin.clubId());
    }
}
