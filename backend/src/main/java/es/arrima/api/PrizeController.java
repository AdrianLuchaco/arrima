package es.arrima.api;

import es.arrima.api.MeleeRequests.StartPrizes;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.prize.PrizeService;
import es.arrima.shared.security.AdminPrincipal;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/melees/{meleeId}/prizes")
class PrizeController {

    private final MeleeWorkflow workflow;
    private final PrizeService prizeService;
    private final MeleeViewAssembler views;

    PrizeController(MeleeWorkflow workflow, PrizeService prizeService, MeleeViewAssembler views) {
        this.workflow = workflow;
        this.prizeService = prizeService;
        this.views = views;
    }

    @PostMapping("/start")
    MeleeView start(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @RequestBody StartPrizes request) {
        workflow.startPrizes(meleeId, admin.clubId(), request.confirmsLosses());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/{prizeId}/awarded")
    MeleeView markAwarded(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long prizeId) {
        prizeService.markAwarded(meleeId, admin.clubId(), prizeId);
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping(path = "/{prizeId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MeleeView addPhoto(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long prizeId, @RequestParam MultipartFile file) throws IOException {
        prizeService.addPhoto(meleeId, admin.clubId(), prizeId, file.getBytes());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @DeleteMapping("/{prizeId}/photos/{photoId}")
    MeleeView deletePhoto(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long prizeId, @PathVariable long photoId) {
        prizeService.deletePhoto(meleeId, admin.clubId(), prizeId, photoId);
        return views.forAdmin(meleeId, admin.clubId());
    }
}
