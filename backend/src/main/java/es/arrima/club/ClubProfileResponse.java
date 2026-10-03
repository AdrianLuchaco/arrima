package es.arrima.club;

/**
 * @param logoUrl signed link to the logo, or null if the club has none
 */
public record ClubProfileResponse(
        String name,
        String logoUrl,
        int courtCount,
        int roundsCount,
        int prizeCount,
        ScoringTableDto scoring) {
}
