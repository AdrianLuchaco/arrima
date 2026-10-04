package es.arrima.push;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * The "Authorization: vapid t=..., k=..." header of RFC 8292: a short JWT signed with our private
 * key (ES256, with Nimbus, already used for the sessions) for the push service of that endpoint.
 */
final class VapidAuthorization {

    /** The RFC allows at most 24 hours. */
    private static final Duration VALIDITY = Duration.ofHours(12);

    private VapidAuthorization() {
    }

    /** @param subject how the push service can contact us: "mailto:..." or an https URL */
    static String header(URI endpoint, VapidKeys keys, String subject, Instant now) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(endpoint.getScheme() + "://" + endpoint.getAuthority())
                .expirationTime(Date.from(now.plus(VALIDITY)))
                .subject(subject)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new ECDSASigner(keys.privateKey()));
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign the VAPID token", e);
        }
        return "vapid t=" + jwt.serialize() + ", k=" + keys.publicKeyBase64Url();
    }
}
