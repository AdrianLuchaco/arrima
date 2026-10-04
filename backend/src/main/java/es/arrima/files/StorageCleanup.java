package es.arrima.files;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Deletes stored files only once the database change that stopped using them has committed: if
 * the transaction rolls back, the rows still point to the files and they must still exist. A file
 * that fails to delete only wastes a little storage, so failures are logged and ignored.
 */
@Component
public class StorageCleanup {

    private static final Logger log = LoggerFactory.getLogger(StorageCleanup.class);

    private final FileStorage fileStorage;

    public StorageCleanup(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public void deleteAfterCommit(List<String> paths) {
        if (paths.isEmpty()) {
            return;
        }
        List<String> toDelete = List.copyOf(paths);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            toDelete.forEach(this::deleteQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                toDelete.forEach(StorageCleanup.this::deleteQuietly);
            }
        });
    }

    private void deleteQuietly(String path) {
        try {
            fileStorage.delete(path);
        } catch (RuntimeException e) {
            log.warn("Could not delete stored file {}", path, e);
        }
    }
}
