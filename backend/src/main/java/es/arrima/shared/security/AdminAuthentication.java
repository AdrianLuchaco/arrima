package es.arrima.shared.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/** A request authenticated with a valid access token. */
class AdminAuthentication extends AbstractAuthenticationToken {

    private final AdminPrincipal principal;
    private final Jwt token;

    AdminAuthentication(AdminPrincipal principal, Jwt token) {
        super(List.of());
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public AdminPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }
}
