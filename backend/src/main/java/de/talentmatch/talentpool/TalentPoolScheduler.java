package de.talentmatch.talentpool;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.talentmatch.talentpool.TalentPoolService.MaintenanceResult;

/**
 * Erinnert an offene Evidenz-Anfragen und löscht abgelaufene – stündlich und einmal beim Start (falls die App
 * zwischenzeitlich nicht lief).
 */
@Component
public class TalentPoolScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TalentPoolScheduler.class);

    private final TalentPoolService talentPoolService;
    private final Clock clock;

    public TalentPoolScheduler(TalentPoolService talentPoolService, Clock clock) {
        this.talentPoolService = talentPoolService;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "0 5 * * * *")
    public void hourly() {
        run();
    }

    private void run() {
        MaintenanceResult result = talentPoolService.runMaintenance(LocalDateTime.now(clock));
        if (result.reminded() + result.deletedUnanswered() + result.deletedExpired() > 0) {
            LOG.info("Evidenz: {} Erinnerung(en), {} unbeantwortete und {} abgelaufene Bewerbung(en) gelöscht.",
                    result.reminded(), result.deletedUnanswered(), result.deletedExpired());
        }
    }
}
