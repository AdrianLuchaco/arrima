package es.arrima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Arrima authenticates club admins with its own JWT flow: Spring's default in-memory user
// (and the generated password it prints in the logs) must not exist.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ArrimaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArrimaApplication.class, args);
    }
}
