package es.arrima.files;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Object names are generated here: a random UUID and the extension of the detected image type.
 * The user's file name is never used, so it cannot carry path traversal or odd characters.
 */
public final class StoredFilePaths {

    private static final Pattern SAFE_PATH = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*\\.(jpg|png)");

    private StoredFilePaths() {
    }

    public static String clubLogo(long clubId, ImageType type) {
        return "clubs/%d/logo-%s.%s".formatted(clubId, UUID.randomUUID(), type.extension());
    }

    public static String prizePhoto(long clubId, long meleeId, ImageType type) {
        return "clubs/%d/melees/%d/%s.%s".formatted(clubId, meleeId, UUID.randomUUID(), type.extension());
    }

    static boolean isSafe(String path) {
        return path != null && SAFE_PATH.matcher(path).matches();
    }
}
