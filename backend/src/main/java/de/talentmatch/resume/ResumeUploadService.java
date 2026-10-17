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
import de.talentmatch.candidate.Candidate;
import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.job.JobPosition;
import de.talentmatch.job.JobPositionRepository;

/**
 * Nimmt PDF-Lebensläufe entgegen: Validierung → Text-Extraktion (Tika) → neue Bewerbung mit Status NEW.
 * Die KI-Analyse wird anschließend separat über {@code POST /api/applications/{id}/analyze} angestoßen.
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

    @Transactional
    public ApplicationSummaryDto upload(Long jobId, MultipartFile file) {
        JobPosition job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Stelle", jobId));
        byte[] data = validate(file);
        String text = textExtractor.extractText(data);
        if (text.isBlank()) {
            throw new InvalidFileException("Im PDF wurde kein Text gefunden (gescanntes Dokument?).");
        }

        String fileName = file.getOriginalFilename() == null ? "Lebenslauf.pdf" : file.getOriginalFilename();
        ResumeHeuristics.Name name = ResumeHeuristics.guessName(fileName);
        Candidate candidate = new Candidate(name.firstName(), name.lastName());
        candidate.setRawResumeText(text);
        ResumeHeuristics.findEmail(text).ifPresent(candidate::setEmail);
        ResumeHeuristics.findPhone(text).ifPresent(candidate::setPhone);

        Application application = new Application(job, candidate, LocalDateTime.now(clock));
        application.setResumeDocument(new ResumeDocument(fileName, PDF_CONTENT_TYPE, data));
        return ApplicationMapper.toSummary(applicationRepository.save(application));
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
