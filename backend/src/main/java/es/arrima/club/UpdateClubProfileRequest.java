package es.arrima.club;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The name is checked again after cleaning, in ClubService (see TextInput). */
public record UpdateClubProfileRequest(
        @NotNull @Size(max = 200) String name,
        @NotNull @Min(1) @Max(MeleeSettings.MAX_COURTS) Integer courtCount,
        @NotNull @Min(1) @Max(MeleeSettings.MAX_ROUNDS) Integer roundsCount,
        @NotNull @Min(1) @Max(MeleeSettings.MAX_PRIZES) Integer prizeCount,
        @NotNull @Min(0) @Max(MeleeSettings.MAX_ENTRY_FEE_CENTS) Integer entryFeeCents,
        @NotNull @Valid ScoringTableDto scoring) {
}
