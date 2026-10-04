package es.arrima.api;

import es.arrima.api.MeleeRequests.ChangeSettings;
import es.arrima.api.MeleeRequests.CreateMelee;
import es.arrima.api.view.MeleeSummary;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.melee.Melee;
import es.arrima.melee.MeleeService;
import es.arrima.melee.NewMelee;
import es.arrima.shared.security.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The club's melees. Every change answers with the whole updated view, so the screen never goes stale. */
@RestController
@RequestMapping("/api/melees")
class MeleeController {

    private final MeleeService meleeService;
    private final MeleeViewAssembler views;

    MeleeController(MeleeService meleeService, MeleeViewAssembler views) {
        this.meleeService = meleeService;
        this.views = views;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MeleeView create(@AuthenticationPrincipal AdminPrincipal admin, @Valid @RequestBody CreateMelee request) {
        Melee melee = meleeService.create(admin.clubId(),
                new NewMelee(request.teamSize(), request.courtCount(), request.roundsCount(), request.prizeCount(),
                        request.entryFeeCents()));
        return views.forAdmin(melee.getId(), admin.clubId());
    }

    @GetMapping
    List<MeleeSummary> list(@AuthenticationPrincipal AdminPrincipal admin) {
        meleeService.closeIdleMelees(admin.clubId());
        return views.summaries(meleeService.listForClub(admin.clubId()));
    }

    @GetMapping("/{meleeId}")
    MeleeView get(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        meleeService.closeIfIdle(meleeId, admin.clubId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PutMapping("/{meleeId}/settings")
    MeleeView changeSettings(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody ChangeSettings request) {
        meleeService.changeSettings(meleeId, admin.clubId(), request.toSettings());
        return views.forAdmin(meleeId, admin.clubId());
    }

    /** One phase back, keeping everything: moving forward again reconciles it. */
    @PostMapping("/{meleeId}/back")
    MeleeView goBack(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        meleeService.goBack(meleeId, admin.clubId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/{meleeId}/close")
    MeleeView close(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        meleeService.close(meleeId, admin.clubId());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @DeleteMapping("/{meleeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId) {
        meleeService.delete(meleeId, admin.clubId());
    }
}
