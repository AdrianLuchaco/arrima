package es.arrima.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.arrima.MutableClock;
import es.arrima.TestSecrets;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.security.SecurityProperties;
import es.arrima.shared.security.SigningKeys;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FileLinkSignerTest {

    private static final String PATH = "clubs/1/logo-abc.png";
    private static final Pattern LINK = Pattern.compile("/api/files/(.+)\\?expires=(\\d+)&signature=(.+)");

    private final MutableClock clock = new MutableClock();
    private final FileLinkSigner signer = new FileLinkSigner(new SigningKeys(new SecurityProperties(
            TestSecrets.randomJwtSecret(), null, null, null, true)), clock);

    @Test
    void aLinkItSignedIsValid() {
        Matcher link = parse(signer.link(PATH));

        assertThatCode(() -> signer.verify(link.group(1), Long.parseLong(link.group(2)), link.group(3)))
                .doesNotThrowAnyException();
    }

    @Test
    void theSignatureDoesNotWorkForAnotherFile() {
        Matcher link = parse(signer.link(PATH));

        assertThatThrownBy(() -> signer.verify("clubs/2/logo-abc.png", Long.parseLong(link.group(2)), link.group(3)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void anExtendedExpiryInvalidatesTheSignature() {
        Matcher link = parse(signer.link(PATH));

        assertThatThrownBy(() -> signer.verify(PATH, Long.parseLong(link.group(2)) + 3600, link.group(3)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void linksExpire() {
        Matcher link = parse(signer.link(PATH));

        clock.advance(Duration.ofHours(13));

        assertThatThrownBy(() -> signer.verify(PATH, Long.parseLong(link.group(2)), link.group(3)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void theSameImageKeepsTheSameLinkForHoursSoBrowsersCanCacheIt() {
        clock.set(Instant.parse("2026-10-03T07:00:00Z"));
        String morning = signer.link(PATH);

        clock.set(Instant.parse("2026-10-03T11:59:00Z"));

        assertThat(signer.link(PATH)).isEqualTo(morning);
    }

    @Test
    void noPathMeansNoLink() {
        assertThat(signer.link(null)).isNull();
    }

    private static Matcher parse(String link) {
        Matcher matcher = LINK.matcher(link);
        assertThat(matcher.matches()).isTrue();
        return matcher;
    }
}
