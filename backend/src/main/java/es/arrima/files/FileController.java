package es.arrima.files;

import es.arrima.shared.error.ApiException;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Serves images from the private storage to anyone holding a valid signed link. */
@RestController
class FileController {

    private final FileLinkSigner linkSigner;
    private final FileStorage storage;

    FileController(FileLinkSigner linkSigner, FileStorage storage) {
        this.linkSigner = linkSigner;
        this.storage = storage;
    }

    @GetMapping("/api/files/{*path}")
    ResponseEntity<byte[]> file(@PathVariable String path, @RequestParam long expires, @RequestParam String signature) {
        String objectPath = path.substring(1); // {*path} captures the leading "/"
        linkSigner.verify(objectPath, expires, signature);
        if (!StoredFilePaths.isSafe(objectPath)) {
            throw ApiException.notFound();
        }
        StoredFile file = storage.load(objectPath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePrivate())
                .body(file.content());
    }
}
