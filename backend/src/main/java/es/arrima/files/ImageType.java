package es.arrima.files;

/** The image formats we accept. The browser compresses photos to JPEG; logos may keep PNG transparency. */
public enum ImageType {

    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png");

    private final String extension;
    private final String contentType;

    ImageType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }
}
