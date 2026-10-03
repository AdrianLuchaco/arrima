package es.arrima.files;

import es.arrima.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Supabase Storage through its REST API, authenticated with the project's secret key.
 * The bucket is private: images reach browsers only through our signed links (FileController).
 */
class SupabaseFileStorage implements FileStorage {

    private final RestClient client;
    private final String bucket;

    SupabaseFileStorage(RestClient.Builder builder, StorageProperties properties) {
        this.bucket = properties.bucket();
        this.client = builder
                .baseUrl(properties.supabaseUrl().replaceAll("/+$", "") + "/storage/v1")
                .defaultHeaders(headers -> authenticate(headers, properties.supabaseKey()))
                .build();
    }

    /**
     * New secret keys (sb_secret_...) go in the "apikey" header only. Legacy service_role keys are
     * JWTs and Storage also expects them as a bearer token.
     */
    private static void authenticate(HttpHeaders headers, String key) {
        headers.set("apikey", key);
        if (!key.startsWith("sb_secret_")) {
            headers.setBearerAuth(key);
        }
    }

    @Override
    public void store(String path, byte[] content, String contentType) {
        client.post()
                .uri(objectUri(path))
                .contentType(MediaType.parseMediaType(contentType))
                .header("x-upsert", "false")
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public StoredFile load(String path) {
        ResponseEntity<byte[]> response;
        try {
            response = client.get()
                    .uri(objectUri(path))
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (HttpClientErrorException e) {
            // Supabase answers 400 or 404 for a missing object.
            throw ApiException.notFound();
        }
        MediaType contentType = response.getHeaders().getContentType();
        return new StoredFile(response.getBody(), contentType == null ? "application/octet-stream" : contentType.toString());
    }

    @Override
    public void delete(String path) {
        client.method(HttpMethod.DELETE)
                .uri("/object/" + bucket)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("prefixes", List.of(requireSafe(path))))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Object paths contain "/" that must reach Supabase as-is (a URI template would encode them).
     * Concatenating is safe because every path is checked against the whitelist first.
     */
    private String objectUri(String path) {
        return "/object/" + bucket + "/" + requireSafe(path);
    }

    private static String requireSafe(String path) {
        if (!StoredFilePaths.isSafe(path)) {
            throw new IllegalArgumentException("Unexpected storage path: " + path);
        }
        return path;
    }
}
