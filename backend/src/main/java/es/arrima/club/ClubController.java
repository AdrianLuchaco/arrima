package es.arrima.club;

import es.arrima.shared.security.AdminPrincipal;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The admin's own club. There is no club id in the URL: it always comes from the access token. */
@RestController
@RequestMapping("/api/club")
class ClubController {

    private final ClubService clubService;

    ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    ClubProfileResponse profile(@AuthenticationPrincipal AdminPrincipal admin) {
        return clubService.getProfile(admin.clubId());
    }

    @PutMapping
    ClubProfileResponse updateProfile(@AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody UpdateClubProfileRequest request) {
        return clubService.updateProfile(admin.clubId(), request);
    }

    @PostMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ClubProfileResponse uploadLogo(@AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam MultipartFile file) throws IOException {
        return clubService.changeLogo(admin.clubId(), file.getBytes());
    }

    @DeleteMapping("/logo")
    ClubProfileResponse removeLogo(@AuthenticationPrincipal AdminPrincipal admin) {
        return clubService.removeLogo(admin.clubId());
    }
}
