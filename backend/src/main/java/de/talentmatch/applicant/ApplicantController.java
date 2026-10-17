package de.talentmatch.applicant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.applicant.api.MyApplicationDto;
import de.talentmatch.applicant.api.OpenJobDto;

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

    @GetMapping("/applications")
    public List<MyApplicationDto> myApplications(@AuthenticationPrincipal Jwt jwt) {
        return applicantService.listMyApplications(jwt.getSubject());
    }

    @PostMapping(path = "/jobs/{jobId}/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MyApplicationDto apply(@AuthenticationPrincipal Jwt jwt, @PathVariable Long jobId,
                                  @RequestPart("file") MultipartFile file) {
        return applicantService.apply(jwt.getSubject(), jobId, file);
    }
}
