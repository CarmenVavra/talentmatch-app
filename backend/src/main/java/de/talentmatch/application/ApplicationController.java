package de.talentmatch.application;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.analysis.AnalysisService;
import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.application.api.ApplicationSummaryDto;
import de.talentmatch.application.api.UpdateNotesRequest;
import de.talentmatch.application.api.UpdateQuestionRequest;
import de.talentmatch.application.api.UpdateStatusRequest;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.resume.DocumentType;
import de.talentmatch.resume.ResumeFile;
import de.talentmatch.resume.ResumeUploadService;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private static final String APPLICATION = "Bewerbung";

    private final ApplicationService applicationService;
    private final ResumeUploadService uploadService;
    private final AnalysisService analysisService;

    public ApplicationController(ApplicationService applicationService, ResumeUploadService uploadService,
                                 AnalysisService analysisService) {
        this.applicationService = applicationService;
        this.uploadService = uploadService;
        this.analysisService = analysisService;
    }

    @GetMapping("/jobs/{jobId}/applications")
    public List<ApplicationSummaryDto> listForJob(@PathVariable Long jobId) {
        return applicationService.listForJob(jobId);
    }

    @PostMapping(path = "/jobs/{jobId}/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationSummaryDto upload(@PathVariable Long jobId, @RequestPart("file") MultipartFile file) {
        return uploadService.upload(jobId, file);
    }

    @GetMapping("/applications")
    public List<ApplicationSummaryDto> listAll() {
        return applicationService.listAll();
    }

    @GetMapping("/applications/{id}")
    public ApplicationDetailDto getDetail(@PathVariable Long id) {
        return applicationService.findDetail(id).orElseThrow(() -> new ResourceNotFoundException(APPLICATION, id));
    }

    @PatchMapping("/applications/{id}")
    public ApplicationSummaryDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return applicationService.updateStatus(id, request.status());
    }

    @PutMapping("/applications/{id}/notes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotes(@PathVariable Long id, @Valid @RequestBody UpdateNotesRequest request) {
        applicationService.updateNotes(id, request.notes());
    }

    @PatchMapping("/applications/{id}/questions/{index}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markQuestion(@PathVariable Long id, @PathVariable int index,
                             @RequestBody UpdateQuestionRequest request) {
        applicationService.markQuestion(id, index, request.discussed());
    }

    @PostMapping("/applications/{id}/analyze")
    public ApplicationDetailDto analyze(@PathVariable Long id) {
        return analysisService.analyze(id);
    }

    @PostMapping("/applications/{id}/questions/generate")
    public ApplicationDetailDto generateQuestions(@PathVariable Long id) {
        return analysisService.generateMoreQuestions(id);
    }

    @GetMapping("/applications/{id}/resume")
    public ResponseEntity<byte[]> resume(@PathVariable Long id) {
        return document(id, DocumentType.RESUME);
    }

    @GetMapping("/applications/{id}/skill-profile")
    public ResponseEntity<byte[]> skillProfile(@PathVariable Long id) {
        return document(id, DocumentType.SKILL_PROFILE);
    }

    @GetMapping("/applications/{id}/cover-letter")
    public ResponseEntity<byte[]> coverLetter(@PathVariable Long id) {
        return document(id, DocumentType.COVER_LETTER);
    }

    private ResponseEntity<byte[]> document(Long id, DocumentType type) {
        ResumeFile file = applicationService.findDocument(id, type)
                .orElseThrow(() -> new ResourceNotFoundException(type.label() + "-PDF der Bewerbung", id));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .body(file.data());
    }
}
