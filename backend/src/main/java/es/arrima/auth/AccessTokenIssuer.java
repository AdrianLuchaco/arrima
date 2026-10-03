package es.arrima.auth;

import es.arrima.shared.security.JwtClaims;
import es.arrima.shared.security.SecurityProperties;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Signs short-lived access tokens: who the admin is and which club they manage. */
@Component
class AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    AccessTokenIssuer(JwtEncoder jwtEncoder, SecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    String issue(ClubAdmin admin, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtClaims.ISSUER)
                .subject(admin.getId().toString())
                .claim(JwtClaims.CLUB_ID, admin.getClubId())
                .issuedAt(now)
                .expiresAt(expiresAt(now))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    Instant expiresAt(Instant now) {
        return now.plus(properties.accessTokenTtl());
    }
}
