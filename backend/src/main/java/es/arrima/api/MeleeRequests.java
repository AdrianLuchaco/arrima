package es.arrima.api;

import es.arrima.club.MeleeSettings;
import es.arrima.international.domain.ThrowOutcome;
import es.arrima.participant.NewParticipant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request bodies of the melee endpoints. Names are checked again after cleaning, in the services. */
final class MeleeRequests {

    private MeleeRequests() {
    }

    /** Null settings mean "the club's default". */
    record CreateMelee(
            @NotNull @Min(2) @Max(3) Integer teamSize,
            @Min(1) @Max(MeleeSettings.MAX_COURTS) Integer courtCount,
            @Min(1) @Max(MeleeSettings.MAX_ROUNDS) Integer roundsCount,
            @Min(1) @Max(MeleeSettings.MAX_PRIZES) Integer prizeCount) {
    }

    record ChangeSettings(
            @NotNull @Min(1) @Max(MeleeSettings.MAX_COURTS) Integer courtCount,
            @NotNull @Min(1) @Max(MeleeSettings.MAX_ROUNDS) Integer roundsCount,
            @NotNull @Min(1) @Max(MeleeSettings.MAX_PRIZES) Integer prizeCount) {

        MeleeSettings toSettings() {
            return new MeleeSettings(courtCount, roundsCount, prizeCount);
        }
    }

    record ParticipantData(Integer listNumber, @NotNull @Size(max = 200) String name) {

        NewParticipant toNewParticipant() {
            return new NewParticipant(listNumber, name);
        }
    }

    record PreviewImport(@NotNull @Size(max = 30_000) String text) {
    }

    record ImportParticipants(@NotNull @Size(max = 300) List<@Valid @NotNull ParticipantData> participants) {
    }

    /** Optional flags: missing means false (Jackson 3 would otherwise reject a missing primitive). */
    record DrawTeams(Boolean acceptDifferentTeam, Boolean confirmLosses) {

        boolean acceptsDifferentTeam() {
            return Boolean.TRUE.equals(acceptDifferentTeam);
        }

        boolean confirmsLosses() {
            return Boolean.TRUE.equals(confirmLosses);
        }
    }

    record SwapPlayers(@NotNull Long firstPlayerId, @NotNull Long secondPlayerId) {
    }

    record Substitute(@NotNull Long leavingPlayerId, @NotNull Long joiningPlayerId) {
    }

    record GenerateSchedule(Boolean confirmLosses) {

        boolean confirmsLosses() {
            return Boolean.TRUE.equals(confirmLosses);
        }
    }

    /** null winner clears the result. */
    record SetWinner(Long winnerTeamId) {
    }

    record AssignCourt(@NotNull @Min(1) @Max(MeleeSettings.MAX_COURTS) Integer courtNumber) {
    }

    record StartInternational(Boolean confirmLosses) {

        boolean confirmsLosses() {
            return Boolean.TRUE.equals(confirmLosses);
        }
    }

    record RecordThrow(@NotNull ThrowOutcome outcome) {
    }
}
