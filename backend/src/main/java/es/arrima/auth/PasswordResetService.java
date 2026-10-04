package es.arrima.auth;

import es.arrima.club.ClubService;
import es.arrima.mail.EmailDispatcher;
import es.arrima.mail.MailProperties;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.shared.ratelimit.RateLimitPolicy;
import es.arrima.shared.ratelimit.RateLimiter;
import es.arrima.shared.security.SecureTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "¿Has olvidado la contraseña?": an e-mail with a link that lets the admin choose a new one.
 * <ul>
 *   <li>The answer to a request is the same whether or not the e-mail has an account, and so is its
 *       timing (the e-mail goes out in the background): nobody can find out who is registered.</li>
 *   <li>The link carries 256 random bits; only their hash is stored. It works once, for 30 minutes,
 *       and asking for another one cancels it.</li>
 *   <li>The token travels after a "#" in the link: browsers never send that part to any server,
 *       so it doesn't end up in Vercel's or Render's logs.</li>
 *   <li>Changing the password ends every open session, in case someone else had one.</li>
 * </ul>
 */
@Service
class PasswordResetService {

    static final Duration LINK_VALIDITY = Duration.ofMinutes(30);

    private final ClubAdminRepository adminRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final ClubService clubService;
    private final EmailDispatcher emailDispatcher;
    private final MailProperties mailProperties;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    PasswordResetService(ClubAdminRepository adminRepository, PasswordResetTokenRepository tokenRepository,
            RefreshTokenService refreshTokenService, ClubService clubService, EmailDispatcher emailDispatcher,
            MailProperties mailProperties, PasswordEncoder passwordEncoder, RateLimiter rateLimiter, Clock clock) {
        this.adminRepository = adminRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.clubService = clubService;
        this.emailDispatcher = emailDispatcher;
        this.mailProperties = mailProperties;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    void requestReset(String rawEmail, String clientIp) {
        String email = AuthService.normalizeEmail(rawEmail);
        rateLimiter.consume(RateLimitPolicy.PASSWORD_RESET_PER_IP, clientIp);
        rateLimiter.consume(RateLimitPolicy.PASSWORD_RESET_PER_EMAIL, email);
        Instant now = clock.instant();
        tokenRepository.deleteExpired(now);

        adminRepository.findByEmail(email).ifPresent(admin -> {
            tokenRepository.deleteAllOfAdmin(admin.getId());
            String rawToken = SecureTokens.generate();
            tokenRepository.save(new PasswordResetToken(admin.getId(), SecureTokens.sha256Hex(rawToken),
                    now, now.plus(LINK_VALIDITY)));
            String clubName = clubService.getClub(admin.getClubId()).getName();
            emailDispatcher.sendAfterCommit(
                    PasswordResetEmail.build(admin.getEmail(), clubName, resetLink(rawToken), LINK_VALIDITY));
        });
    }

    @Transactional
    void resetPassword(String rawToken, String newPassword, String clientIp) {
        rateLimiter.consume(RateLimitPolicy.PASSWORD_RESET_CONFIRM_PER_IP, clientIp);
        // Checked before the link is spent, so a too-short password can simply be corrected.
        PasswordPolicy.check(newPassword, "password");
        Instant now = clock.instant();

        PasswordResetToken token = tokenRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_LINK_INVALID));
        ClubAdmin admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_LINK_INVALID));

        admin.changePasswordHash(passwordEncoder.encode(newPassword), now);
        token.markUsed(now);
        refreshTokenService.revokeAllOfAdmin(admin.getId(), now);
    }

    private String resetLink(String rawToken) {
        return mailProperties.appUrl().replaceAll("/+$", "") + "/restablecer#" + rawToken;
    }
}
