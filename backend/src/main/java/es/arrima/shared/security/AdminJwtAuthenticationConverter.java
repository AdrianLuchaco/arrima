package es.arrima.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

/** Turns a validated JWT (signature, expiry and issuer already checked) into an {@link AdminPrincipal}. */
@Component
class AdminJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Number clubId = jwt.getClaim(JwtClaims.CLUB_ID);
        if (jwt.getSubject() == null || clubId == null) {
            throw new InvalidBearerTokenException("Access token without admin or club");
        }
        AdminPrincipal principal = new AdminPrincipal(Long.parseLong(jwt.getSubject()), clubId.longValue());
        return new AdminAuthentication(principal, jwt);
    }
}
