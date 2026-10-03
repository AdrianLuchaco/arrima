package es.arrima.files;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param provider       "supabase" in production, "local" for development (files in a folder)
 * @param localDirectory folder used by the local provider
 * @param supabaseUrl    project URL, e.g. https://abcdefghijklmnop.supabase.co
 * @param supabaseKey    secret key (sb_secret_...): it stays in the backend, never reaches the browser
 * @param bucket         private bucket that holds every image
 */
@ConfigurationProperties("arrima.storage")
public record StorageProperties(
        String provider,
        String localDirectory,
        String supabaseUrl,
        String supabaseKey,
        String bucket) {
}
