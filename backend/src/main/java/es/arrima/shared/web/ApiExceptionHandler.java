package es.arrima.shared.web;

import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem responses with a machine-readable "code" property.
 * The parent class already covers Spring MVC's own exceptions (bad requests, 404, 405...).
 * Clients never receive stack traces or internal messages: unexpected errors are logged here and
 * the response only says what the client can act on.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        ProblemDetail problem = problem(exception.code());
        exception.details().forEach(problem::setProperty);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.code().status());
        if (exception.details().get("retryAfterSeconds") instanceof Long seconds) {
            response.header(HttpHeaders.RETRY_AFTER, seconds.toString());
        }
        return response.body(problem);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    ProblemDetail handleDatabaseUnavailable(DataAccessResourceFailureException exception) {
        log.error("Database unavailable", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The database is not available. Try again in a moment.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** Bean Validation errors: one entry per invalid field, so the form can mark each one. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getCode()));
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED);
        problem.setProperty("fields", fields);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(ErrorCode.IMAGE_TOO_LARGE.status()).body(problem(ErrorCode.IMAGE_TOO_LARGE));
    }

    private static ProblemDetail problem(ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatus(code.status());
        problem.setProperty("code", code.name());
        return problem;
    }
}
