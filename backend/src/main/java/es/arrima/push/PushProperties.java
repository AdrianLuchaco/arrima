package es.arrima.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param vapidPublicKey  base64url raw public key (VAPID_PUBLIC_KEY)
 * @param vapidPrivateKey base64url raw private key (VAPID_PRIVATE_KEY): it stays in the backend
 * @param subject         contact for the push services, "mailto:..." (VAPID_SUBJECT)
 */
@ConfigurationProperties("arrima.push")
public record PushProperties(String vapidPublicKey, String vapidPrivateKey, String subject) {
}
