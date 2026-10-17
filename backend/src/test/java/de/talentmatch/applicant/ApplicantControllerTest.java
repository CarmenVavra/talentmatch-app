package de.talentmatch.applicant;

import static de.talentmatch.support.WebMvcSecurityTestConfig.applicant;
import static de.talentmatch.support.WebMvcSecurityTestConfig.hrManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.talentmatch.applicant.api.MyApplicationDto;
import de.talentmatch.applicant.api.OpenJobDto;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.resume.ApplicantDocuments;
import de.talentmatch.support.WebMvcSecurityTestConfig;

@WebMvcTest(ApplicantController.class)
@Import(WebMvcSecurityTestConfig.class)
class ApplicantControllerTest {

    private static final MyApplicationDto APPLICATION = new MyApplicationDto(1L, 3L, "Data Engineer", "Data",
            "Hamburg", ApplicantStatus.INTERVIEW, LocalDateTime.of(2026, 10, 8, 10, 15), "cv.pdf", null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicantService applicantService;

    @Test
    void applicantSeesOwnApplicationsWithStatusOnly() throws Exception {
        when(applicantService.listMyApplications("bewerber@talentmatch.de")).thenReturn(List.of(APPLICATION));

        mockMvc.perform(get("/api/applicant/applications").with(applicant()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INTERVIEW"))
                .andExpect(jsonPath("$[0].overallScore").doesNotExist())
                .andExpect(jsonPath("$[0].analysis").doesNotExist());
    }

    @Test
    void returnsSingleOpenJobWithFullDescription() throws Exception {
        when(applicantService.findOpenJob("bewerber@talentmatch.de", 3L)).thenReturn(Optional.of(
                new OpenJobDto(3L, "Data Engineer", "Data", "Mid", "Hamburg", "Lange Beschreibung.",
                        List.of("Python"), LocalDate.of(2026, 9, 21), false)));

        mockMvc.perform(get("/api/applicant/jobs/3").with(applicant()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Lange Beschreibung."))
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.averageScore").doesNotExist());
    }

    @Test
    void unknownOpenJobReturns404() throws Exception {
        when(applicantService.findOpenJob("bewerber@talentmatch.de", 99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/applicant/jobs/99").with(applicant()))
                .andExpect(status().isNotFound());
    }

    @Test
    void hrManagerHasNoAccessToThePortal() throws Exception {
        mockMvc.perform(get("/api/applicant/jobs").with(hrManager())).andExpect(status().isForbidden());
    }

    @Test
    void portalRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/applicant/jobs")).andExpect(status().isUnauthorized());
    }

    @Test
    void applyReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {37});
        when(applicantService.apply(eq("bewerber@talentmatch.de"), eq(3L), any())).thenReturn(APPLICATION);

        mockMvc.perform(multipart("/api/applicant/jobs/3/applications").file(file).with(applicant()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobTitle").value("Data Engineer"));
    }

    @Test
    void applyPassesOptionalSkillProfileAndCoverLetter() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {37});
        MockMultipartFile skills = new MockMultipartFile("skillProfile", "skills.pdf", "application/pdf",
                new byte[] {37});
        MockMultipartFile letter = new MockMultipartFile("coverLetter", "brief.pdf", "application/pdf",
                new byte[] {37});
        ArgumentCaptor<ApplicantDocuments> documents = ArgumentCaptor.forClass(ApplicantDocuments.class);
        when(applicantService.apply(eq("bewerber@talentmatch.de"), eq(3L), documents.capture()))
                .thenReturn(APPLICATION);

        mockMvc.perform(multipart("/api/applicant/jobs/3/applications").file(file).file(skills).file(letter)
                        .with(applicant()))
                .andExpect(status().isCreated());

        assertThat(documents.getValue().resume().getOriginalFilename()).isEqualTo("cv.pdf");
        assertThat(documents.getValue().skillProfile()).hasValueSatisfying(
                f -> assertThat(f.getOriginalFilename()).isEqualTo("skills.pdf"));
        assertThat(documents.getValue().coverLetter()).hasValueSatisfying(
                f -> assertThat(f.getOriginalFilename()).isEqualTo("brief.pdf"));
    }

    @Test
    void applyWithResumeOnlyLeavesOptionalDocumentsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {37});
        ArgumentCaptor<ApplicantDocuments> documents = ArgumentCaptor.forClass(ApplicantDocuments.class);
        when(applicantService.apply(eq("bewerber@talentmatch.de"), eq(3L), documents.capture()))
                .thenReturn(APPLICATION);

        mockMvc.perform(multipart("/api/applicant/jobs/3/applications").file(file).with(applicant()))
                .andExpect(status().isCreated());

        assertThat(documents.getValue().skillProfile()).isEmpty();
        assertThat(documents.getValue().coverLetter()).isEmpty();
    }

    @Test
    void duplicateApplicationReturns409() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {37});
        when(applicantService.apply(eq("bewerber@talentmatch.de"), eq(3L), any()))
                .thenThrow(new BusinessRuleException("Sie haben sich auf diese Stelle bereits beworben."));

        mockMvc.perform(multipart("/api/applicant/jobs/3/applications").file(file).with(applicant()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sie haben sich auf diese Stelle bereits beworben."));
    }
}
