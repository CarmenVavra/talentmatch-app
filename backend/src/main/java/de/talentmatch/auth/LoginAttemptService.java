package de.talentmatch.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import de.talentmatch.common.exception.LoginLockedException;
import de.talentmatch.config.TalentMatchProperties;

/**
 * Schutz gegen Passwort-Raten: Nach {@code max-login-failures} Fehlversuchen in Folge ist die Anmeldung für die
 * E-Mail-Adresse {@code login-lock-duration} lang gesperrt – auch mit dem richtigen Passwort. Eine erfolgreiche
 * Anmeldung oder ein zurückgesetztes Passwort hebt den Zähler auf. Der Zustand liegt im Speicher und ist nach einem
 * Neustart leer.
 */
@Service
public class LoginAttemptService {

    private record Failures(int count, Instant lockedUntil) {
    }

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final TalentMatchProperties.Security settings;
    private final Clock clock;

    public LoginAttemptService(TalentMatchProperties properties, Clock clock) {
        this.settings = properties.security();
        this.clock = clock;
    }

    /**
     * @throws LoginLockedException wenn die Anmeldung für diese Adresse gerade gesperrt ist
     */
    public void checkNotLocked(String email) {
        Failures current = failures.get(key(email));
        Instant now = clock.instant();
        if (current != null && current.lockedUntil() != null && now.isBefore(current.lockedUntil())) {
            long minutes = Math.max(1, (Duration.between(now, current.lockedUntil()).toSeconds() + 59) / 60);
            throw new LoginLockedException("Zu viele fehlgeschlagene Anmeldeversuche. Bitte versuchen Sie es in "
                    + minutes + (minutes == 1 ? " Minute" : " Minuten") + " erneut oder setzen Sie Ihr Passwort "
                    + "zurück.");
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        failures.compute(key(email), (ignored, current) -> {
            boolean lockExpired = current != null && current.lockedUntil() != null
                    && !now.isBefore(current.lockedUntil());
            int count = current == null || lockExpired ? 1 : current.count() + 1;
            Instant lockedUntil = count >= settings.maxLoginFailures()
                    ? now.plus(settings.loginLockDuration()) : null;
            return new Failures(count, lockedUntil);
        });
    }

    public void reset(String email) {
        failures.remove(key(email));
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
