package es.arrima.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Password length is checked by PasswordPolicy, in bytes as well as characters. */
record RegisterRequest(
        @NotBlank @Size(max = 64) String invitationCode,
        @NotNull @Size(max = 200) String clubName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(max = 128) String password) {
}
