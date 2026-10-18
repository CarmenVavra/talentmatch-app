package de.talentmatch.privacy;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Prüft die Aufbewahrungsfrist abgesagter Bewerbungen – stündlich und einmal beim Start.
 */
@Component
public class RetentionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionScheduler.class);

    private final RetentionService retentionService;
    private final Clock clock;

    public RetentionScheduler(RetentionService retentionService, Clock clock) {
        this.retentionService = retentionService;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "0 10 * * * *")
    public void hourly() {
        run();
    }

    private void run() {
        int deleted = retentionService.deleteExpiredRejections(LocalDateTime.now(clock));
        if (deleted > 0) {
            LOG.info("Datenschutz: {} abgesagte Bewerbung(en) nach Ablauf der Aufbewahrungsfrist gelöscht.",
                    deleted);
        }
    }
}
