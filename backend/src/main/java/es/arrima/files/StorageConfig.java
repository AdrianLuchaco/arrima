package es.arrima.files;

import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfig {

    @Bean
    FileStorage fileStorage(StorageProperties properties) {
        return switch (properties.provider()) {
            case "supabase" -> {
                requireSet(properties.supabaseUrl(), "SUPABASE_URL");
                requireSet(properties.supabaseKey(), "SUPABASE_SECRET_KEY");
                yield new SupabaseFileStorage(RestClient.builder(), properties);
            }
            case "local" -> new LocalDiskFileStorage(Path.of(properties.localDirectory()));
            default -> throw new IllegalStateException(
                    "STORAGE_PROVIDER must be 'supabase' or 'local', not '" + properties.provider() + "'");
        };
    }

    private static void requireSet(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variable + " is required when STORAGE_PROVIDER=supabase");
        }
    }
}
