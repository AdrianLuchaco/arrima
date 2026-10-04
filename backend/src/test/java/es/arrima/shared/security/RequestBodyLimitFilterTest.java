package es.arrima.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestBodyLimitFilterTest {

    private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter();

    @Test
    void rejectsADeclaredBodyThatIsTooLargeWithoutReadingIt() throws Exception {
        MockHttpServletRequest request = jsonRequest(new byte[(int) RequestBodyLimitFilter.MAX_JSON_BYTES + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("must not reach the application");
        });

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("REQUEST_TOO_LARGE");
    }

    @Test
    void stopsReadingABodySentInChunksOnceItIsTooLarge() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        request.setContent(new byte[(int) RequestBodyLimitFilter.MAX_JSON_BYTES + 10]);
        FilterChain readsEverything = (req, res) -> req.getInputStream().readAllBytes();

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), readsEverything))
                .isInstanceOf(IOException.class);
    }

    @Test
    void letsNormalBodiesAndUploadsThrough() throws Exception {
        MockHttpServletRequest json = jsonRequest(new byte[1000]);
        MockHttpServletRequest upload = new MockHttpServletRequest("POST", "/api/club/logo");
        upload.setContentType("multipart/form-data; boundary=x");
        upload.setContent(new byte[(int) RequestBodyLimitFilter.MAX_JSON_BYTES * 4]);
        int[] reached = {0};

        filter.doFilter(json, new MockHttpServletResponse(), (req, res) -> reached[0]++);
        filter.doFilter(upload, new MockHttpServletResponse(), (req, res) -> reached[0]++);

        assertThat(reached[0]).isEqualTo(2);
    }

    private static MockHttpServletRequest jsonRequest(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContentType("application/json");
        request.setContent(body);
        return request;
    }
}
