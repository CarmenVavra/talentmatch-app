package de.talentmatch.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.application.api.ApplicationSummaryDto;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.JobPositionRepository;
import de.talentmatch.resume.ResumeFile;

/**
 * Lesen und Bearbeiten von Bewerbungen (Status, Notizen, Interview-Checkliste).
 */
@Service
public class ApplicationService {

    private static final String APPLICATION = "Bewerbung";

    private final ApplicationRepository applicationRepository;
    private final JobPositionRepository jobRepository;

    public ApplicationService(ApplicationRepository applicationRepository, JobPositionRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummaryDto> listForJob(Long jobId) {
        var job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Stelle", jobId));
        return applicationRepository.findByJobPositionIdOrderByCreatedAtDesc(job.getId()).stream()
                .map(ApplicationMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummaryDto> listAll() {
        return applicationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ApplicationMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationDetailDto> findDetail(Long id) {
        return applicationRepository.findWithDetailsById(id).map(ApplicationMapper::toDetail);
    }

    @Transactional
    public ApplicationSummaryDto updateStatus(Long id, ApplicationStatus status) {
        Application application = loadWithDetails(id);
        application.setStatus(status);
        return ApplicationMapper.toSummary(application);
    }

    @Transactional
    public void updateNotes(Long id, String notes) {
        loadWithDetails(id).setInterviewNotes(notes == null ? null : notes.strip());
    }

    @Transactional
    public void markQuestion(Long id, int index, boolean discussed) {
        Application application = loadWithDetails(id);
        application.getAnalysis()
                .orElseThrow(() -> new BusinessRuleException("Für diese Bewerbung liegt noch keine KI-Analyse vor."))
                .markQuestion(index, discussed);
    }

    @Transactional(readOnly = true)
    public Optional<ResumeFile> findResume(Long id) {
        return applicationRepository.findWithResumeById(id)
                .flatMap(Application::getResumeDocument)
                .map(doc -> new ResumeFile(doc.getFileName(), doc.getContentType(), doc.getData()));
    }

    private Application loadWithDetails(Long id) {
        return applicationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION, id));
    }
}
