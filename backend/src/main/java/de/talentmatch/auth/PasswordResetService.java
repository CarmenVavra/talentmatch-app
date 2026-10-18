package de.talentmatch.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.common.exception.InvalidTokenException;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.mail.MailService;

/**
 * „Passwort vergessen“: verschickt einen Einmal-Link per E-Mail und setzt darüber ein neues Passwort.
 * Ob es zur Adresse ein Konto gibt, verrät die Anfrage nicht.
 */
@Service
public class PasswordResetService {

    static final String RESET_PATH = "/passwort-zuruecksetzen";
    static final String INVALID_LINK =
            "Der Link ist ungültig oder abgelaufen. Bitte fordern Sie einen neuen Link an.";
    /** Mindestabstand zwischen zwei Mails an dieselbe Adresse. */
    private static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final int TOKEN_BYTES = 32;

    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final MailService mailService;
    private final TalentMatchProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserAccountRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, LoginAttemptService loginAttempts,
                                MailService mailService, TalentMatchProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.mailService = mailService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Legt einen neuen Link an und verschickt ihn – nur wenn ein Konto existiert und die letzte Mail mindestens
     * eine Minute zurückliegt. Ältere Links des Kontos werden ungültig.
     */
    @Transactional
    public void requestReset(String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.deleteExpired(now);
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> !tokenRepository.existsByUserAndCreatedAtAfter(user, now.minus(COOLDOWN)))
                .ifPresent(user -> {
                    tokenRepository.deleteByUser(user);
                    String token = newToken();
                    Duration validity = properties.security().passwordResetValidity();
                    tokenRepository.save(new PasswordResetToken(user, hash(token), now, now.plus(validity)));
                    mailService.queue(user.getEmail(), "Passwort zurücksetzen",
                            mailBody(user, resetUrl(token), validity), null);
                });
    }

    /**
     * Setzt das neue Passwort. Der Link ist danach verbraucht, eine Anmeldesperre wird aufgehoben.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .filter(candidate -> candidate.isValidAt(now))
                .orElseThrow(() -> new InvalidTokenException(INVALID_LINK));
        UserAccount user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        tokenRepository.deleteByUser(user);
        loginAttempts.reset(user.getEmail());
    }

    private String resetUrl(String token) {
        String base = properties.mail().appBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + RESET_PATH + "?token=" + token;
    }

    private static String mailBody(UserAccount user, String url, Duration validity) {
        return """
                Guten Tag %s %s,

                für Ihr TalentMatch-Konto wurde ein neues Passwort angefordert. Über diesen Link können Sie es \
                festlegen:
                %s

                Der Link ist %d Minuten gültig und kann nur einmal verwendet werden. Wenn Sie kein neues Passwort \
                angefordert haben, ignorieren Sie diese E-Mail einfach – Ihr bisheriges Passwort bleibt gültig.

                Mit freundlichen Grüßen
                Ihr Recruiting-Team""".formatted(user.getFirstName(), user.getLastName(), url,
                validity.toMinutes());
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nicht verfügbar", ex);
        }
    }
}
