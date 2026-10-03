package es.arrima.melee;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Codes for the public link /m/{code}: 8 characters from 31 symbols (no 0/O, 1/I/L, so they can be
 * read aloud or typed from a sheet of paper): about 8.5·10^11 combinations. With the rate limit on
 * failed look-ups, guessing a code is not feasible.
 */
@Component
public class PublicCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final MeleeRepository meleeRepository;

    public PublicCodeGenerator(MeleeRepository meleeRepository) {
        this.meleeRepository = meleeRepository;
    }

    public String newCode() {
        String code;
        do {
            code = randomCode();
        } while (meleeRepository.existsByPublicCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
