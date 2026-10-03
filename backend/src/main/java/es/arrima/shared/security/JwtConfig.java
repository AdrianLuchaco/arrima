package es.arrima.shared.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Access tokens are HS256 JWTs: the same service signs and verifies them, so a shared secret is
 * enough and there is no key pair to publish. Spring's Nimbus integration does the cryptography.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
class JwtConfig {

    @Bean
    JwtEncoder jwtEncoder(SigningKeys signingKeys) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKeys.jwtKey()));
    }

    @Bean
    JwtDecoder jwtDecoder(SigningKeys signingKeys, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKeys.jwtKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator expiry = new JwtTimestampValidator(Duration.ofSeconds(30));
        expiry.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(expiry, new JwtIssuerValidator(JwtClaims.ISSUER)));
        return decoder;
    }
}
