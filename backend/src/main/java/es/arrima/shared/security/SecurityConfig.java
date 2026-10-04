package es.arrima.shared.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import es.arrima.shared.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AdminJwtAuthenticationConverter jwtConverter)
            throws Exception {
        http
                // No CSRF tokens: the API authenticates with a bearer token in the Authorization header,
                // which browsers never attach on their own. The refresh cookie has its own protection.
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new RequestBodyLimitFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterBefore(new CrossSiteRequestFilter(), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(GET, "/api/health", "/api/health/db").permitAll()
                        .requestMatchers(POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/password-reset/request", "/api/auth/password-reset/confirm").permitAll()
                        // Images: access is granted by the signature in the link (see FileLinkSigner).
                        .requestMatchers(GET, "/api/files/**").permitAll()
                        // Public melee view: read-only by construction (only GET is allowed).
                        .requestMatchers(GET, "/api/public/**").permitAll()
                        .requestMatchers("/api/club", "/api/club/**").authenticated()
                        .requestMatchers("/api/melees", "/api/melees/**").authenticated()
                        // Deny by default: every endpoint must be listed above.
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityProblemResponses.write(response, ErrorCode.UNAUTHENTICATED)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityProblemResponses.write(response, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityProblemResponses.write(response, ErrorCode.NOT_FOUND)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // The API only returns JSON and images: nothing may be loaded or framed from them.
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    /** BCrypt today, with the "{bcrypt}" prefix stored so the algorithm can be upgraded later. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
