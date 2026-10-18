package de.talentmatch.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.mail.OutgoingMailRepository;

/**
 * Löscht Bewerbungen endgültig – mit Kandidat:in, Unterlagen, KI-Analyse und den zugehörigen E-Mails im
 * Postausgang, damit keine personenbezogenen Daten zurückbleiben.
 */
@Service
public class ApplicationDeletionService {

    private final ApplicationRepository applicationRepository;
    private final OutgoingMailRepository mailRepository;

    public ApplicationDeletionService(ApplicationRepository applicationRepository,
                                      OutgoingMailRepository mailRepository) {
        this.applicationRepository = applicationRepository;
        this.mailRepository = mailRepository;
    }

    @Transactional
    public void delete(Long id) {
        delete(applicationRepository.findWithDocumentsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bewerbung", id)));
    }

    @Transactional
    public void delete(Application application) {
        deleteAll(List.of(application));
    }

    @Transactional
    public void deleteAll(List<Application> applications) {
        if (applications.isEmpty()) {
            return;
        }
        mailRepository.deleteByApplicationIds(applications.stream().map(Application::getId).toList());
        applicationRepository.deleteAll(applications);
    }
}
