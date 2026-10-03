package es.arrima.auth;

import es.arrima.auth.RefreshTokenService.IssuedRefreshToken;
import es.arrima.auth.RefreshTokenService.RotatedRefreshToken;
import es.arrima.club.Club;
import es.arrima.club.ClubService;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.shared.ratelimit.RateLimitPolicy;
import es.arrima.shared.ratelimit.RateLimiter;
import es.arrima.shared.security.SecureTokens;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

    private final ClubAdminRepository adminRepository;
    private final SignupInvitationRepository invitationRepository;
    private final ClubService clubService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    /**
     * Hash compared against when the e-mail does not exist, so a failed login takes the same time
     * whether or not the account exists (otherwise response times would reveal registered e-mails).
     */
    private final String timingDummyHash;

    AuthService(ClubAdminRepository adminRepository, SignupInvitationRepository invitationRepository,
            ClubService clubService, RefreshTokenService refreshTokenService, AccessTokenIssuer accessTokenIssuer,
            PasswordEncoder passwordEncoder, RateLimiter rateLimiter, Clock clock) {
        this.adminRepository = adminRepository;
        this.invitationRepository = invitationRepository;
        this.clubService = clubService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.timingDummyHash = passwordEncoder.encode("timing-dummy-password");
    }

    /**
     * Creates the club and its admin account, consuming the invitation. The invitation is checked
     * first, so without a valid code nobody can even find out whether an e-mail is registered.
     */
    @Transactional
    IssuedTokens register(RegisterRequest request, String clientIp) {
        rateLimiter.consume(RateLimitPolicy.REGISTER_PER_IP, clientIp);
        Instant now = clock.instant();

        String codeHash = SecureTokens.sha256Hex(SignupInvitation.normalizeCode(request.invitationCode()));
        SignupInvitation invitation = invitationRepository.findByCodeHash(codeHash)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_INVALID));

        String email = normalizeEmail(request.email());
        if (adminRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        PasswordPolicy.check(request.password(), "password");

        Club club = clubService.createClub(request.clubName());
        ClubAdmin admin = adminRepository.save(
                new ClubAdmin(club.getId(), email, passwordEncoder.encode(request.password()), now));
        invitation.markUsed(club.getId(), now);
        admin.recordLogin(now);
        return issueTokens(admin, refreshTokenService.startFamily(admin.getId(), now), now);
    }

    /** Same error for a wrong e-mail and a wrong password: the response never reveals which. */
    @Transactional
    IssuedTokens login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        rateLimiter.consume(RateLimitPolicy.LOGIN_PER_IP, clientIp);
        rateLimiter.consume(RateLimitPolicy.LOGIN_PER_EMAIL, email);
        Instant now = clock.instant();

        ClubAdmin admin = adminRepository.findByEmail(email).orElse(null);
        String storedHash = admin == null ? timingDummyHash : admin.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), storedHash);
        if (admin == null || !passwordMatches) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (passwordEncoder.upgradeEncoding(storedHash)) {
            admin.changePasswordHash(passwordEncoder.encode(request.password()), now);
        }
        admin.recordLogin(now);
        return issueTokens(admin, refreshTokenService.startFamily(admin.getId(), now), now);
    }

    /**
     * noRollbackFor: when a stolen token is detected the family is revoked and then the request is
     * rejected with an ApiException. Rolling back would undo the revocation.
     */
    @Transactional(noRollbackFor = ApiException.class)
    IssuedTokens refresh(String rawRefreshToken, String clientIp) {
        rateLimiter.consume(RateLimitPolicy.REFRESH_PER_IP, clientIp);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Instant now = clock.instant();
        RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken, now);
        ClubAdmin admin = adminRepository.findById(rotated.adminId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        return issueTokens(admin, rotated.replacement(), now);
    }

    @Transactional
    void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeFamilyOf(rawRefreshToken, clock.instant());
        }
    }

    private IssuedTokens issueTokens(ClubAdmin admin, IssuedRefreshToken refreshToken, Instant now) {
        return new IssuedTokens(accessTokenIssuer.issue(admin, now), accessTokenIssuer.expiresAt(now),
                refreshToken.value(), refreshToken.expiresAt());
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
