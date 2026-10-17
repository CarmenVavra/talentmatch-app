package de.talentmatch.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anwendungsspezifische Einstellungen unter dem Präfix {@code talentmatch}.
 */
@ConfigurationProperties(prefix = "talentmatch")
public record TalentMatchProperties(Cors cors, Jwt jwt, Upload upload, Seed seed) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, Duration expiration) {
    }

    public record Upload(long maxFileSizeBytes) {
    }

    public record Seed(
            boolean enabled,
            String hrEmail,
            String hrPassword,
            String applicantEmail,
            String applicantPassword) {
    }
}
