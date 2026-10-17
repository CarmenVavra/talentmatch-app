package de.talentmatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Aktiviert {@code @Async} (z. B. automatische KI-Analyse nach einer Portal-Bewerbung).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
