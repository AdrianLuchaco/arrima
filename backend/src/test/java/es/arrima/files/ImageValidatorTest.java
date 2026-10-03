package es.arrima.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ImageValidatorTest {

    private final ImageValidator validator = new ImageValidator();

    @Test
    void acceptsRealJpegAndPng() {
        assertThat(validator.validate(TestImages.jpeg(40, 30))).isEqualTo(ImageType.JPEG);
        assertThat(validator.validate(TestImages.png(40, 30))).isEqualTo(ImageType.PNG);
    }

    @Test
    void rejectsAFileThatOnlyPretendsToBeAnImage() {
        byte[] script = "<script>alert('hola')</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(script)).satisfies(e -> hasCode(e, ErrorCode.INVALID_IMAGE));
    }

    @Test
    void rejectsATruncatedImage() {
        byte[] png = TestImages.png(40, 30);
        byte[] truncated = java.util.Arrays.copyOf(png, 12);

        assertThatThrownBy(() -> validator.validate(truncated)).satisfies(e -> hasCode(e, ErrorCode.INVALID_IMAGE));
    }

    @Test
    void rejectsAHugeImageWithoutDecodingIt() {
        // A valid PNG header declaring 50,000 x 50,000 pixels: decoding it would need gigabytes.
        byte[] png = TestImages.png(1, 1);
        ByteBuffer.wrap(png, 16, 8).putInt(50_000).putInt(50_000);

        assertThatThrownBy(() -> validator.validate(png)).satisfies(e -> hasCode(e, ErrorCode.INVALID_IMAGE));
    }

    @Test
    void rejectsFilesOverTheSizeLimit() {
        byte[] tooBig = new byte[ImageValidator.MAX_BYTES + 1];

        assertThatThrownBy(() -> validator.validate(tooBig)).satisfies(e -> hasCode(e, ErrorCode.IMAGE_TOO_LARGE));
    }

    private static void hasCode(Throwable exception, ErrorCode code) {
        assertThat(exception).isInstanceOf(ApiException.class);
        assertThat(((ApiException) exception).code()).isEqualTo(code);
    }
}
