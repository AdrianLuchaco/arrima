package es.arrima.api;

import es.arrima.api.MeleeRequests.ImportParticipants;
import es.arrima.api.MeleeRequests.ParticipantData;
import es.arrima.api.MeleeRequests.PreviewImport;
import es.arrima.api.view.MeleeView;
import es.arrima.api.view.MeleeViewAssembler;
import es.arrima.participant.ImportPreviewEntry;
import es.arrima.participant.ParticipantService;
import es.arrima.shared.security.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sign-up list of a melee: WhatsApp import with preview, and manual changes. */
@RestController
@RequestMapping("/api/melees/{meleeId}/participants")
class ParticipantController {

    private final ParticipantService participantService;
    private final MeleeViewAssembler views;

    ParticipantController(ParticipantService participantService, MeleeViewAssembler views) {
        this.participantService = participantService;
        this.views = views;
    }

    /** Parses the pasted text and returns the editable preview; nothing is saved. */
    @PostMapping("/preview")
    List<ImportPreviewEntry> preview(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody PreviewImport request) {
        return participantService.previewImport(meleeId, admin.clubId(), request.text());
    }

    @PostMapping("/import")
    MeleeView importConfirmed(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody ImportParticipants request) {
        participantService.importParticipants(meleeId, admin.clubId(),
                request.participants().stream().map(ParticipantData::toNewParticipant).toList());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MeleeView add(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @Valid @RequestBody ParticipantData request) {
        participantService.add(meleeId, admin.clubId(), request.toNewParticipant());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PutMapping("/{participantId}")
    MeleeView edit(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long participantId, @Valid @RequestBody ParticipantData request) {
        participantService.edit(meleeId, admin.clubId(), participantId, request.toNewParticipant());
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/{participantId}/withdraw")
    MeleeView withdraw(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long participantId) {
        participantService.withdraw(meleeId, admin.clubId(), participantId);
        return views.forAdmin(meleeId, admin.clubId());
    }

    @PostMapping("/{participantId}/reinstate")
    MeleeView reinstate(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long participantId) {
        participantService.reinstate(meleeId, admin.clubId(), participantId);
        return views.forAdmin(meleeId, admin.clubId());
    }

    @DeleteMapping("/{participantId}")
    MeleeView delete(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable long meleeId,
            @PathVariable long participantId) {
        participantService.delete(meleeId, admin.clubId(), participantId);
        return views.forAdmin(meleeId, admin.clubId());
    }
}
