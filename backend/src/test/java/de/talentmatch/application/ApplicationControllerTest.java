package de.talentmatch.application;

import static de.talentmatch.support.WebMvcSecurityTestConfig.hrManager;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.talentmatch.analysis.AnalysisService;
import de.talentmatch.application.api.ApplicationSummaryDto;
import de.talentmatch.application.api.CandidateSummaryDto;
import de.talentmatch.common.exception.AiAnalysisException;
import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.resume.DocumentType;
import de.talentmatch.resume.ResumeFile;
import de.talentmatch.resume.ResumeUploadService;
import de.talentmatch.support.WebMvcSecurityTestConfig;

@WebMvcTest(ApplicationController.class)
@Import(WebMvcSecurityTestConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private ResumeUploadService uploadService;

    @MockitoBean
    private AnalysisService analysisService;

    @Test
    void listsApplicationsOfJob() throws Exception {
        when(applicationService.listForJob(1L)).thenReturn(List.of(summary(ApplicationStatus.MATCHED)));

        mockMvc.perform(get("/api/jobs/1/applications").with(hrManager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidate.lastName").value("Weber"))
                .andExpect(jsonPath("$[0].overallScore").value(84));
    }

    @Test
    void patchUpdatesStatus() throws Exception {
        when(applicationService.updateStatus(5L, ApplicationStatus.INTERVIEW))
                .thenReturn(summary(ApplicationStatus.INTERVIEW));

        mockMvc.perform(patch("/api/applications/5").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void patchRejectsUnknownStatus() throws Exception {
        mockMvc.perform(patch("/api/applications/5").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadMapsInvalidFileTo400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bild.png", "image/png", new byte[] {1, 2, 3});
        when(uploadService.upload(eq(1L), any())).thenThrow(new InvalidFileException("Falscher Dateityp"));

        mockMvc.perform(multipart("/api/jobs/1/applications").file(file).with(hrManager()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Falscher Dateityp"));
    }

    @Test
    void analyzeMapsAiFailureTo502() throws Exception {
        when(analysisService.analyze(5L)).thenThrow(new AiAnalysisException("Kein OpenAI-API-Key konfiguriert."));

        mockMvc.perform(post("/api/applications/5/analyze").with(hrManager()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("KI-Analyse fehlgeschlagen"));
    }

    @Test
    void markSeenReturns204() throws Exception {
        mockMvc.perform(post("/api/applications/5/seen").with(hrManager()))
                .andExpect(status().isNoContent());
        verify(applicationService).markSeen(5L);
    }

    @Test
    void markQuestionReturns204() throws Exception {
        mockMvc.perform(patch("/api/applications/5/questions/2").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"discussed\":true}"))
                .andExpect(status().isNoContent());
        verify(applicationService).markQuestion(5L, 2, true);
    }

    @Test
    void resumeIsServedInlineAsPdf() throws Exception {
        when(applicationService.findDocument(5L, DocumentType.RESUME))
                .thenReturn(Optional.of(new ResumeFile("cv.pdf", "application/pdf", new byte[] {37, 80, 68, 70})));

        mockMvc.perform(get("/api/applications/5/resume").with(hrManager()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"cv.pdf\""));
    }

    @Test
    void skillProfileIsServedInlineAsPdf() throws Exception {
        when(applicationService.findDocument(5L, DocumentType.SKILL_PROFILE)).thenReturn(
                Optional.of(new ResumeFile("skills.pdf", "application/pdf", new byte[] {37, 80, 68, 70})));

        mockMvc.perform(get("/api/applications/5/skill-profile").with(hrManager()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"skills.pdf\""));
    }

    @Test
    void missingCoverLetterReturns404() throws Exception {
        when(applicationService.findDocument(5L, DocumentType.COVER_LETTER)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/applications/5/cover-letter").with(hrManager()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("Anschreiben")));
    }

    private static ApplicationSummaryDto summary(ApplicationStatus status) {
        return new ApplicationSummaryDto(5L, 1L, "Senior Java Developer", status, LocalDateTime.of(2026, 10, 6, 9, 0),
                new CandidateSummaryDto(7L, "Jonas", "Weber", "Java Developer", "Siemens", "Erlangen"), 84,
                List.of("Java", "Spring Boot"), false);
    }
}
