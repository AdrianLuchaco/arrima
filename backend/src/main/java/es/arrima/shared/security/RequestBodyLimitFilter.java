package es.arrima.shared.security;

import es.arrima.shared.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps the size of JSON bodies. Spring reads a JSON body whole before validating it, so without a
 * cap anyone (login needs no account) could send hundreds of megabytes and exhaust the 512 MB of
 * the free instance. The largest real body, importing 300 players, is about 240 KB.
 * <p>
 * Uploads (multipart) are left to their own limits: spring.servlet.multipart and ImageValidator.
 */
class RequestBodyLimitFilter extends OncePerRequestFilter {

    static final long MAX_JSON_BYTES = 512 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_JSON_BYTES) {
            SecurityProblemResponses.write(response, ErrorCode.REQUEST_TOO_LARGE);
            return;
        }
        // A body sent in chunks has no declared length: it is counted while it is read instead.
        chain.doFilter(request.getContentLengthLong() < 0 ? new LimitedBodyRequest(request) : request, response);
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

        private LimitedBodyRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedInputStream(super.getInputStream());
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream body;
        private long bytesRead;

        private LimitedInputStream(ServletInputStream body) {
            this.body = body;
        }

        @Override
        public int read() throws IOException {
            int value = body.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = body.read(buffer, offset, length);
            if (read > 0) {
                count(read);
            }
            return read;
        }

        private void count(int bytes) throws IOException {
            bytesRead += bytes;
            if (bytesRead > MAX_JSON_BYTES) {
                throw new IOException("Request body larger than " + MAX_JSON_BYTES + " bytes");
            }
        }

        @Override
        public boolean isFinished() {
            return body.isFinished();
        }

        @Override
        public boolean isReady() {
            return body.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            body.setReadListener(listener);
        }
    }
}
