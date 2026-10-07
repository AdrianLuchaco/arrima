package es.arrima.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes sent to the client in the "code" property of every error response.
 * The frontend translates them into Spanish; the backend never sends user-facing text.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    CROSS_SITE_REQUEST(HttpStatus.FORBIDDEN),
    REQUEST_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),

    // Authentication
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
    INVITATION_INVALID(HttpStatus.BAD_REQUEST),
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    /** The password-recovery link does not exist, has expired or was already used. */
    RESET_LINK_INVALID(HttpStatus.BAD_REQUEST),

    // Melees
    INVALID_STATE(HttpStatus.CONFLICT),
    CONFIRMATION_REQUIRED(HttpStatus.CONFLICT),
    /** Someone's payment is still unmarked: the draw asks before recording them as not paid. */
    UNMARKED_PAYMENTS(HttpStatus.CONFLICT),
    /** Another round's countdown has not ended: starting this one stops it, once confirmed. */
    TIMER_RUNNING(HttpStatus.CONFLICT),
    PARTICIPANT_LIMIT(HttpStatus.BAD_REQUEST),
    NOT_ENOUGH_PLAYERS(HttpStatus.CONFLICT),
    TEAMS_DO_NOT_FIT(HttpStatus.CONFLICT),
    TEAMS_INCOMPLETE(HttpStatus.CONFLICT),
    TOO_MANY_ROUNDS(HttpStatus.CONFLICT),
    /** More matches per round than courts: some are played off court, once the admin accepts it. */
    OFF_COURT_MATCHES(HttpStatus.CONFLICT),
    NO_TEAMS(HttpStatus.CONFLICT),
    NO_SCHEDULE(HttpStatus.CONFLICT),
    COURT_OCCUPIED(HttpStatus.CONFLICT),
    RESULTS_MISSING(HttpStatus.CONFLICT),
    TOO_MANY_VIEWERS(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNATIONAL_INCOMPLETE(HttpStatus.CONFLICT),
    PHOTO_LIMIT(HttpStatus.CONFLICT),
    PUSH_LIMIT(HttpStatus.CONFLICT),

    // Files
    INVALID_IMAGE(HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),
    INVALID_FILE_LINK(HttpStatus.FORBIDDEN);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
