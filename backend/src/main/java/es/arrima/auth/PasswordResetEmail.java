package es.arrima.auth;

import es.arrima.mail.Email;
import java.time.Duration;
import org.springframework.web.util.HtmlUtils;

/**
 * The e-mail with the link to choose a new password, in Spanish like the rest of the app.
 * The club name is written by its admin: it is escaped before going into the HTML.
 */
final class PasswordResetEmail {

    private static final String SUBJECT = "Cambia la contraseña de Arrima";

    private PasswordResetEmail() {
    }

    static Email build(String to, String clubName, String link, Duration validity) {
        long minutes = validity.toMinutes();
        String text = """
                Hola:

                Alguien (seguramente tú) ha pedido cambiar la contraseña de Arrima del club %s.

                Para elegir una nueva, abre este enlace en los próximos %d minutos:
                %s

                Si no lo has pedido tú, no hagas nada: tu contraseña sigue siendo la misma.
                """.formatted(clubName, minutes, link);
        String html = """
                <p>Hola:</p>
                <p>Alguien (seguramente tú) ha pedido cambiar la contraseña de Arrima del club <strong>%s</strong>.</p>
                <p><a href="%s">Elegir una contraseña nueva</a> (el enlace caduca en %d minutos).</p>
                <p>Si no lo has pedido tú, no hagas nada: tu contraseña sigue siendo la misma.</p>
                """.formatted(HtmlUtils.htmlEscape(clubName), HtmlUtils.htmlEscape(link), minutes);
        return new Email(to, SUBJECT, text, html);
    }
}
