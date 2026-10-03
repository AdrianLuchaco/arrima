package es.arrima.files;

import es.arrima.shared.error.ApiException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Development storage: files in a local folder, so the app runs without a Supabase project.
 * Not for production: Render's disk is wiped on every deploy.
 */
class LocalDiskFileStorage implements FileStorage {

    private final Path root;

    LocalDiskFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(String path, byte[] content, String contentType) {
        try {
            Path file = resolve(path);
            Files.createDirectories(file.getParent());
            Files.write(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public StoredFile load(String path) {
        try {
            Path file = resolve(path);
            String contentType = path.endsWith(".png") ? ImageType.PNG.contentType() : ImageType.JPEG.contentType();
            return new StoredFile(Files.readAllBytes(file), contentType);
        } catch (NoSuchFileException e) {
            throw ApiException.notFound();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String path) {
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Path outside the storage folder: " + path);
        }
        return file;
    }
}
