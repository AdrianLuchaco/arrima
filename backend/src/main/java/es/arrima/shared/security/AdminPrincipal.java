package es.arrima.shared.security;

/**
 * The authenticated club administrator, read from the access token. Controllers receive it with
 * {@code @AuthenticationPrincipal}: this is the only source of the club id, never the URL or the body.
 */
public record AdminPrincipal(long adminId, long clubId) {
}
