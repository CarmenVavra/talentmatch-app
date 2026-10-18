package de.talentmatch.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anwendungsspezifische Einstellungen unter dem Präfix {@code talentmatch}.
 */
@ConfigurationProperties(prefix = "talentmatch")
public record TalentMatchProperties(Cors cors, Jwt jwt, Upload upload, Seed seed, Mail mail,
                                    TalentPool talentPool, Privacy privacy,
                                    Security security) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, Duration expiration) {
    }

    public record Upload(long maxFileSizeBytes) {
    }

    /**
     * Absender der E-Mails und Basis-URL der App für Links in E-Mails.
     */
    public record Mail(String from, String appBaseUrl) {
    }

    /**
     * Evidenz: Antwortfrist, Erinnerung nach und Aufbewahrungsdauer nach Zustimmung.
     */
    public record TalentPool(Duration responsePeriod, Duration reminderAfter, Duration retention) {
    }

    /**
     * Datenschutz: Aufbewahrungsdauer abgesagter Bewerbungen ohne Evidenz.
     */
    public record Privacy(Duration rejectedRetention) {
    }

    /**
     * Schutz der Anmeldung: Sperre nach zu vielen Fehlversuchen und Gültigkeit der Links zum Zurücksetzen.
     */
    public record Security(int maxLoginFailures, Duration loginLockDuration, Duration passwordResetValidity) {
    }

    public record Seed(
            boolean enabled,
            String hrEmail,
            String hrPassword,
            String applicantEmail,
            String applicantPassword) {
    }
}
