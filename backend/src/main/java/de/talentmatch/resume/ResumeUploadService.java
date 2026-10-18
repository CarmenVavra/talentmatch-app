package de.talentmatch.resume;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationMapper;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.api.ApplicationSummaryDto;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.JobPosition;
import de.talentmatch.job.JobPositionRepository;
import de.talentmatch.resume.PdfDocumentReader.ReadDocument;

/**
 * Nimmt Bewerbungsunterlagen (PDF) entgegen: Validierung → Text-Extraktion (Tika) → neue Bewerbung mit Status NEW.
 * Die KI-Analyse folgt separat (HR: {@code POST /api/applications/{id}/analyze}, Portal: automatisch).
 */
@Service
public class ResumeUploadService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final JobPositionRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final PdfDocumentReader documentReader;
    private final Clock clock;

    public ResumeUploadService(JobPositionRepository jobRepository, ApplicationRepository applicationRepository,
                               PdfDocumentReader documentReader, Clock clock) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.documentReader = documentReader;
        this.clock = clock;
    }

    /**
     * Upload durch HR – Lebenslauf und optional Zusatzunterlagen derselben Person (Skill-Profil, Anschreiben).
     * Der Name wird aus dem Dateinamen des Lebenslaufs, Kontaktdaten aus dem PDF-Text vorbelegt.
     */
    @Transactional
    public ApplicationSummaryDto upload(Long jobId, ApplicantDocuments documents) {
        JobPosition job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Stelle", jobId));
        ResumeHeuristics.Name name = ResumeHeuristics.guessName(
                PdfDocumentReader.fileName(DocumentType.RESUME, documents.resume()));
        Application application = new Application(job, new Candidate(name.firstName(), name.lastName()),
                LocalDateTime.now(clock));
        return ApplicationMapper.toSummary(attachAll(application, documents));
    }

    /**
     * Bewerbung über das Bewerber-Portal: Stammdaten stammen aus dem Benutzerkonto; Skill-Profil und Anschreiben
     * sind optional und werden ebenfalls als PDF gespeichert und ausgelesen.
     */
    @Transactional
    public Application submitForApplicant(JobPosition job, UserAccount applicant, ApplicantDocuments documents) {
        Candidate candidate = new Candidate(applicant.getFirstName(), applicant.getLastName());
        candidate.setEmail(applicant.getEmail());
        Application application = new Application(job, candidate, LocalDateTime.now(clock));
        application.setApplicantAccount(applicant);
        return attachAll(application, documents);
    }

    /**
     * Lebenslauf zuerst (seine Fehler haben Vorrang), danach die optionalen Unterlagen; dann speichern.
     */
    private Application attachAll(Application application, ApplicantDocuments documents) {
        Candidate candidate = application.getCandidate();
        String resumeText = attachDocument(application, DocumentType.RESUME, documents.resume());
        candidate.setRawResumeText(resumeText);
        if (candidate.getEmail() == null) {
            ResumeHeuristics.findEmail(resumeText).ifPresent(candidate::setEmail);
        }
        ResumeHeuristics.findPhone(resumeText).ifPresent(candidate::setPhone);
        documents.skillProfile().ifPresent(file ->
                candidate.setSkillProfileText(attachDocument(application, DocumentType.SKILL_PROFILE, file)));
        documents.coverLetter().ifPresent(file ->
                candidate.setCoverLetterText(attachDocument(application, DocumentType.COVER_LETTER, file)));
        return applicationRepository.save(application);
    }

    /**
     * Validiert das PDF, hinterlegt es an der Bewerbung und liefert den extrahierten Text.
     */
    private String attachDocument(Application application, DocumentType type, MultipartFile file) {
        ReadDocument document = documentReader.read(type, file);
        application.setDocument(type, new ResumeDocument(document.fileName(), PDF_CONTENT_TYPE, document.data()));
        return document.text();
    }
}
