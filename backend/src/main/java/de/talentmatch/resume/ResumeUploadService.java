package de.talentmatch.resume;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationMapper;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.api.ApplicationSummaryDto;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.job.JobPosition;
import de.talentmatch.job.JobPositionRepository;

/**
 * Nimmt PDF-Lebensläufe entgegen: Validierung → Text-Extraktion (Tika) → neue Bewerbung mit Status NEW.
 * Die KI-Analyse folgt separat (HR: {@code POST /api/applications/{id}/analyze}, Portal: automatisch).
 */
@Service
public class ResumeUploadService {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final JobPositionRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeTextExtractor textExtractor;
    private final TalentMatchProperties properties;
    private final Clock clock;

    public ResumeUploadService(JobPositionRepository jobRepository, ApplicationRepository applicationRepository,
                               ResumeTextExtractor textExtractor, TalentMatchProperties properties, Clock clock) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.textExtractor = textExtractor;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Upload durch HR: Name wird aus dem Dateinamen, Kontaktdaten aus dem PDF-Text vorbelegt.
     */
    @Transactional
    public ApplicationSummaryDto upload(Long jobId, MultipartFile file) {
        JobPosition job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Stelle", jobId));
        ResumeHeuristics.Name name = ResumeHeuristics.guessName(fileName(file));
        Application application = createApplication(job, file, new Candidate(name.firstName(), name.lastName()));
        return ApplicationMapper.toSummary(application);
    }

    /**
     * Bewerbung über das Bewerber-Portal: Stammdaten stammen aus dem Benutzerkonto.
     */
    @Transactional
    public Application submitForApplicant(JobPosition job, UserAccount applicant, MultipartFile file) {
        Candidate candidate = new Candidate(applicant.getFirstName(), applicant.getLastName());
        candidate.setEmail(applicant.getEmail());
        Application application = new Application(job, candidate, LocalDateTime.now(clock));
        application.setApplicantAccount(applicant);
        return attachResume(application, file);
    }

    private Application createApplication(JobPosition job, MultipartFile file, Candidate candidate) {
        return attachResume(new Application(job, candidate, LocalDateTime.now(clock)), file);
    }

    private Application attachResume(Application application, MultipartFile file) {
        byte[] data = validate(file);
        String text = textExtractor.extractText(data);
        if (text.isBlank()) {
            throw new InvalidFileException("Im PDF wurde kein Text gefunden (gescanntes Dokument?).");
        }
        Candidate candidate = application.getCandidate();
        candidate.setRawResumeText(text);
        if (candidate.getEmail() == null) {
            ResumeHeuristics.findEmail(text).ifPresent(candidate::setEmail);
        }
        ResumeHeuristics.findPhone(text).ifPresent(candidate::setPhone);
        application.setResumeDocument(new ResumeDocument(fileName(file), PDF_CONTENT_TYPE, data));
        return applicationRepository.save(application);
    }

    private static String fileName(MultipartFile file) {
        return file == null || file.getOriginalFilename() == null ? "Lebenslauf.pdf" : file.getOriginalFilename();
    }

    private byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Die Datei ist leer.");
        }
        if (file.getSize() > properties.upload().maxFileSizeBytes()) {
            throw new InvalidFileException("Die Datei ist größer als 10 MB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidFileException("Die Datei konnte nicht gelesen werden.", ex);
        }
        boolean pdfMagic = data.length >= PDF_MAGIC.length
                && Arrays.equals(Arrays.copyOf(data, PDF_MAGIC.length), PDF_MAGIC);
        if (!name.endsWith(".pdf") || !pdfMagic) {
            throw new InvalidFileException("Falscher Dateityp – nur PDF-Dateien sind erlaubt.");
        }
        return data;
    }
}
