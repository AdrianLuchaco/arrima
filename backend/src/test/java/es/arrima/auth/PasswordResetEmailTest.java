package es.arrima.auth;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.mail.Email;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PasswordResetEmailTest {

    @Test
    void theClubNameCannotInjectHtml() {
        Email email = PasswordResetEmail.build("admin@example.com", "<a href=\"https://malo.example\">Club</a>",
                "https://arrima.test/restablecer#abc", Duration.ofMinutes(30));

        assertThat(email.html()).doesNotContain("<a href=\"https://malo.example\"");
        assertThat(email.html()).contains("&lt;a href=&quot;https://malo.example&quot;&gt;Club&lt;/a&gt;");
        assertThat(email.html()).contains("<a href=\"https://arrima.test/restablecer#abc\">");
        assertThat(email.text()).contains("https://arrima.test/restablecer#abc").contains("30 minutos");
    }
}
