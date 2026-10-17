package de.talentmatch.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import de.talentmatch.applicant.ApplicationSubmittedEvent;

/**
 * Startet die KI-Analyse für Portal-Bewerbungen automatisch im Hintergrund, sobald die Bewerbung gespeichert ist.
 * Schlägt sie fehl (z. B. ohne API-Key), bleibt die Bewerbung „Eingegangen“ und HR kann sie manuell analysieren.
 */
@Component
public class AutoAnalysisListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutoAnalysisListener.class);

    private final AnalysisService analysisService;

    public AutoAnalysisListener(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Async
    @TransactionalEventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        try {
            analysisService.analyze(event.applicationId());
            LOG.info("Automatische KI-Analyse für Bewerbung {} abgeschlossen", event.applicationId());
        } catch (RuntimeException ex) {
            LOG.warn("Automatische KI-Analyse für Bewerbung {} fehlgeschlagen: {}", event.applicationId(),
                    ex.getMessage());
        }
    }
}
