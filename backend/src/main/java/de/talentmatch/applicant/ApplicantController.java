package de.talentmatch.applicant;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.applicant.api.DeleteAccountRequest;
import de.talentmatch.applicant.api.MyApplicationDto;
import de.talentmatch.applicant.api.OpenJobDto;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.resume.ApplicantDocuments;

/**
 * Bewerber-Portal (Rolle APPLICANT). Alle Daten sind auf das angemeldete Konto beschränkt.
 */
@RestController
@RequestMapping("/api/applicant")
public class ApplicantController {

    private final ApplicantService applicantService;

    public ApplicantController(ApplicantService applicantService) {
        this.applicantService = applicantService;
    }

    @GetMapping("/jobs")
    public List<OpenJobDto> openJobs(@AuthenticationPrincipal Jwt jwt) {
        return applicantService.listOpenJobs(jwt.getSubject());
    }

    @GetMapping("/jobs/{jobId}")
    public OpenJobDto openJob(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId) {
        return applicantService.findOpenJob(jwt.getSubject(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Stelle", jobId));
    }

    @GetMapping("/applications")
    public List<MyApplicationDto> myApplications(@AuthenticationPrincipal Jwt jwt) {
        return applicantService.listMyApplications(jwt.getSubject());
    }

    @PostMapping(path = "/jobs/{jobId}/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MyApplicationDto apply(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId,
                                  @RequestPart("file") MultipartFile file,
                                  @RequestPart(name = "skillProfile", required = false) MultipartFile skillProfile,
                                  @RequestPart(name = "coverLetter", required = false) MultipartFile coverLetter) {
        return applicantService.apply(jwt.getSubject(), jobId,
                ApplicantDocuments.of(file, skillProfile, coverLetter));
    }

    @DeleteMapping("/applications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        applicantService.withdraw(jwt.getSubject(), id);
    }

    /**
     * Löscht das Konto samt allen Bewerbungen (POST, weil das Passwort im Body mitgeschickt wird).
     */
    @PostMapping("/account/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        applicantService.deleteAccount(jwt.getSubject(), request.password());
    }
}
