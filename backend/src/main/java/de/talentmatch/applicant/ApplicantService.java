package de.talentmatch.applicant;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.applicant.api.MyApplicationDto;
import de.talentmatch.applicant.api.OpenJobDto;
import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationDeletionService;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.auth.UserAccountRepository;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.InvalidPasswordException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.JobPosition;
import de.talentmatch.job.JobPositionRepository;
import de.talentmatch.resume.ApplicantDocuments;
import de.talentmatch.resume.DocumentType;
import de.talentmatch.resume.ResumeUploadService;
import de.talentmatch.talentpool.TalentPoolConsent;

/**
 * Bewerber-Portal: offene Stellen ansehen, sich bewerben, Status der eigenen Bewerbungen verfolgen.
 */
@Service
public class ApplicantService {

    private final UserAccountRepository userRepository;
    private final JobPositionRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeUploadService uploadService;
    private final ApplicationDeletionService deletionService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public ApplicantService(UserAccountRepository userRepository, JobPositionRepository jobRepository,
                            ApplicationRepository applicationRepository, ResumeUploadService uploadService,
                            ApplicationDeletionService deletionService, PasswordEncoder passwordEncoder,
                            ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.uploadService = uploadService;
        this.deletionService = deletionService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<OpenJobDto> listOpenJobs(String email) {
        UserAccount applicant = loadAccount(email);
        Set<Long> appliedJobIds =
                new HashSet<>(applicationRepository.findJobIdsByApplicantAccountId(applicant.getId()));
        return jobRepository.findAllByOrderByCreatedDateDescIdAsc().stream()
                .map(job -> toOpenJob(job, appliedJobIds.contains(job.getId())))
                .toList();
    }

    /**
     * Detailansicht einer offenen Stelle inklusive „bereits beworben“.
     */
    @Transactional(readOnly = true)
    public Optional<OpenJobDto> findOpenJob(String email, Long jobId) {
        UserAccount applicant = loadAccount(email);
        return jobRepository.findById(jobId).map(job -> toOpenJob(job,
                applicationRepository.existsByJobPositionIdAndApplicantAccountId(jobId, applicant.getId())));
    }

    @Transactional(readOnly = true)
    public List<MyApplicationDto> listMyApplications(String email) {
        UserAccount applicant = loadAccount(email);
        return applicationRepository.findByApplicantAccountIdOrderByCreatedAtDesc(applicant.getId()).stream()
                .map(ApplicantService::toMyApplication)
                .toList();
    }

    /**
     * Reicht eine Bewerbung ein. Die KI-Analyse startet nach dem Commit automatisch im Hintergrund.
     */
    @Transactional
    public MyApplicationDto apply(String email, Long jobId, ApplicantDocuments documents) {
        UserAccount applicant = loadAccount(email);
        JobPosition job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Stelle", jobId));
        if (applicationRepository.existsByJobPositionIdAndApplicantAccountId(jobId, applicant.getId())) {
            throw new BusinessRuleException("Sie haben sich auf diese Stelle bereits beworben.");
        }
        Application application = uploadService.submitForApplicant(job, applicant, documents);
        events.publishEvent(new ApplicationSubmittedEvent(application.getId()));
        return toMyApplication(application);
    }

    /**
     * Zieht eine eigene Bewerbung zurück: Sie wird mit allen Unterlagen, der KI-Analyse und den E-Mails gelöscht.
     */
    @Transactional
    public void withdraw(String email, Long applicationId) {
        UserAccount applicant = loadAccount(email);
        Application application = applicationRepository.findById(applicationId)
                .filter(candidate -> candidate.getApplicantAccount()
                        .map(account -> account.getId().equals(applicant.getId()))
                        .orElse(false))
                .orElseThrow(() -> new ResourceNotFoundException("Bewerbung", applicationId));
        deletionService.delete(application);
    }

    /**
     * Löscht das eigene Konto samt allen Bewerbungen. Zur Sicherheit muss das Passwort bestätigt werden.
     */
    @Transactional
    public void deleteAccount(String email, String password) {
        UserAccount applicant = loadAccount(email);
        if (!passwordEncoder.matches(password, applicant.getPasswordHash())) {
            throw new InvalidPasswordException("Das Passwort ist nicht korrekt.");
        }
        deletionService.deleteAll(applicationRepository.findByApplicantAccountIdOrderByCreatedAtDesc(
                applicant.getId()));
        userRepository.delete(applicant);
    }

    private UserAccount loadAccount(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Benutzer", email));
    }

    private static OpenJobDto toOpenJob(JobPosition job, boolean applied) {
        return new OpenJobDto(job.getId(), job.getTitle(), job.getDepartment(), job.getExperienceLevel(),
                job.getLocation(), job.getDescription(), job.getRequiredSkills(), job.getCreatedDate(), applied);
    }

    private static MyApplicationDto toMyApplication(Application application) {
        JobPosition job = application.getJobPosition();
        return new MyApplicationDto(application.getId(), job.getId(), job.getTitle(), job.getDepartment(),
                job.getLocation(), ApplicantStatus.from(application.getStatus()), application.getCreatedAt(),
                application.getResumeFileName().orElse(null),
                application.getDocumentFileName(DocumentType.SKILL_PROFILE).orElse(null),
                application.getDocumentFileName(DocumentType.COVER_LETTER).orElse(null),
                application.getTalentPool().map(TalentPoolConsent::getStatus).orElse(null),
                application.getTalentPool().map(TalentPoolConsent::getToken).orElse(null),
                application.getTalentPool().map(TalentPoolConsent::getResponseDeadline).orElse(null),
                application.getTalentPool().map(TalentPoolConsent::getExpiresAt).orElse(null));
    }
}
