package es.arrima.auth;

import es.arrima.shared.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
class PasswordResetController {

    private final PasswordResetService passwordResetService;

    PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /** Always 202, whether or not the e-mail has an account. */
    @PostMapping("/request")
    ResponseEntity<Void> request(@Valid @RequestBody ResetRequest request, HttpServletRequest http) {
        passwordResetService.requestReset(request.email(), ClientIp.of(http));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm")
    ResponseEntity<Void> confirm(@Valid @RequestBody NewPassword request, HttpServletRequest http) {
        passwordResetService.resetPassword(request.token(), request.password(), ClientIp.of(http));
        return ResponseEntity.noContent().build();
    }

    record ResetRequest(@NotBlank @Size(max = 254) String email) {
    }

    record NewPassword(@NotBlank @Size(max = 128) String token, @NotNull @Size(max = 128) String password) {
    }
}
