package es.arrima.shared.security;

/**
 * Names shared by the code that issues access tokens and the code that reads them.
 * The subject ("sub") is the admin id.
 */
public final class JwtClaims {

    public static final String ISSUER = "arrima";
    public static final String CLUB_ID = "club";

    private JwtClaims() {
    }
}
