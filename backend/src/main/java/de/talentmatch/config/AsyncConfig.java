package de.talentmatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aktiviert {@code @Async} (z. B. automatische KI-Analyse, E-Mail-Versand) und {@code @Scheduled}
 * (Evidenz-Pflege).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
