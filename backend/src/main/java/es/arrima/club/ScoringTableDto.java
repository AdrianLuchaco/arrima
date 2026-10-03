package es.arrima.club;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Points table as the API exposes and accepts it. */
public record ScoringTableDto(
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer pointingOut,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer pointingBigCircle,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer pointingSmallCircle,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer pointingNearJack,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer pointingOnJack,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer shootingMiss,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer shootingHit,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer shootingHitOut,
        @NotNull @Min(0) @Max(ScoringTable.MAX_POINTS) Integer shootingCarreau) {

    public static ScoringTableDto from(ScoringTable table) {
        return new ScoringTableDto(table.pointingOut(), table.pointingBigCircle(), table.pointingSmallCircle(),
                table.pointingNearJack(), table.pointingOnJack(), table.shootingMiss(), table.shootingHit(),
                table.shootingHitOut(), table.shootingCarreau());
    }

    public ScoringTable toScoringTable() {
        return new ScoringTable(pointingOut, pointingBigCircle, pointingSmallCircle, pointingNearJack, pointingOnJack,
                shootingMiss, shootingHit, shootingHitOut, shootingCarreau);
    }
}
