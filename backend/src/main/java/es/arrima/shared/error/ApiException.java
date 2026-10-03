package es.arrima.shared.error;

import java.util.Map;

/**
 * An expected failure that the client can act on. Its code (and optional details) become the body
 * of the error response; see {@link es.arrima.shared.web.ApiExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public ApiException(ErrorCode code) {
        this(code, Map.of());
    }

    public ApiException(ErrorCode code, Map<String, Object> details) {
        super(code.name());
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public static ApiException validation(String field, String reason) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, Map.of("fields", Map.of(field, reason)));
    }

    public static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
