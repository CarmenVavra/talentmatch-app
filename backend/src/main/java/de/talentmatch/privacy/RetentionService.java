package de.talentmatch.privacy;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationDeletionService;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.config.TalentMatchProperties;

/**
 * Löscht abgesagte Bewerbungen ohne Evidenz nach Ablauf der Aufbewahrungsfrist
 * ({@code talentmatch.privacy.rejected-retention}). Bewerbungen in Evidenz verwaltet die Evidenz selbst.
 */
@Service
public class RetentionService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDeletionService deletionService;
    private final TalentMatchProperties properties;

    public RetentionService(ApplicationRepository applicationRepository, ApplicationDeletionService deletionService,
                            TalentMatchProperties properties) {
        this.applicationRepository = applicationRepository;
        this.deletionService = deletionService;
        this.properties = properties;
    }

    /**
     * @return Anzahl gelöschter Bewerbungen
     */
    @Transactional
    public int deleteExpiredRejections(LocalDateTime now) {
        applicationRepository.startMissingRejectionPeriods(now);
        List<Application> expired =
                applicationRepository.findRejectedBefore(now.minus(properties.privacy().rejectedRetention()));
        deletionService.deleteAll(expired);
        return expired.size();
    }
}
