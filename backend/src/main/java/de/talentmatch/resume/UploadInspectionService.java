package de.talentmatch.resume;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.JobPositionRepository;
import de.talentmatch.resume.PdfDocumentReader.ReadDocument;
import de.talentmatch.resume.api.InspectedFileDto;
import de.talentmatch.resume.api.UploadInspectionDto;

/**
 * Vorabprüfung eines HR-Uploads mit mehreren Dateien: erkennt je Datei Dokumentart, Name und Kontaktdaten und
 * schlägt vor, welche Dateien zu einer Person gehören. Es wird nichts gespeichert.
 */
@Service
public class UploadInspectionService {

    /** Obergrenze je Vorabprüfung – schützt vor versehentlich riesigen Auswahlen. */
    static final int MAX_FILES = 30;

    private final JobPositionRepository jobRepository;
    private final PdfDocumentReader documentReader;

    public UploadInspectionService(JobPositionRepository jobRepository, PdfDocumentReader documentReader) {
        this.jobRepository = jobRepository;
        this.documentReader = documentReader;
    }

    public UploadInspectionDto inspect(Long jobId, List<MultipartFile> files) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Stelle", jobId);
        }
        if (files.size() > MAX_FILES) {
            throw new InvalidFileException("Bitte höchstens " + MAX_FILES + " Dateien auf einmal hochladen.");
        }
        List<InspectedFileDto> inspected = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            inspected.add(inspect(i, files.get(i)));
        }
        return new UploadInspectionDto(inspected, DocumentGrouping.group(inspected));
    }

    private InspectedFileDto inspect(int index, MultipartFile file) {
        String fileName = PdfDocumentReader.fileName(DocumentType.RESUME, file);
        try {
            ReadDocument document = documentReader.read(DocumentType.RESUME, file);
            ResumeHeuristics.Name name = ResumeHeuristics.guessName(fileName);
            return new InspectedFileDto(index, fileName, DocumentClassifier.classify(fileName, document.text()),
                    name.firstName(), name.lastName(),
                    ResumeHeuristics.findEmail(document.text()).orElse(null),
                    ResumeHeuristics.findPhone(document.text()).orElse(null), null);
        } catch (InvalidFileException ex) {
            return new InspectedFileDto(index, fileName, null, null, null, null, null, ex.getMessage());
        }
    }
}
