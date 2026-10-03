package es.arrima.files;

import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/**
 * Checks that an upload really is a reasonable image, whatever name or Content-Type the client sent:
 * <ol>
 *   <li>size limit;</li>
 *   <li>type detected from the first bytes ("magic numbers"), not from the extension;</li>
 *   <li>dimensions read from the header only, without decoding the pixels: a tiny file can declare
 *       a gigantic image (a "decompression bomb") and decoding it would exhaust our 512 MB.</li>
 * </ol>
 */
@Component
public class ImageValidator {

    public static final int MAX_BYTES = 3 * 1024 * 1024;
    private static final int MAX_DIMENSION = 6000;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    public ImageType validate(byte[] content) {
        if (content.length == 0) {
            throw new ApiException(ErrorCode.INVALID_IMAGE);
        }
        if (content.length > MAX_BYTES) {
            throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
        }
        ImageType type = detectType(content);
        checkDimensions(content);
        return type;
    }

    private static ImageType detectType(byte[] content) {
        if (startsWith(content, JPEG_SIGNATURE)) {
            return ImageType.JPEG;
        }
        if (startsWith(content, PNG_SIGNATURE)) {
            return ImageType.PNG;
        }
        throw new ApiException(ErrorCode.INVALID_IMAGE);
    }

    private static void checkDimensions(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ApiException(ErrorCode.INVALID_IMAGE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new ApiException(ErrorCode.INVALID_IMAGE);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_IMAGE);
        }
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }
}
