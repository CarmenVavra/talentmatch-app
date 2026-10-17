package de.talentmatch.job;

import static de.talentmatch.support.WebMvcSecurityTestConfig.applicant;
import static de.talentmatch.support.WebMvcSecurityTestConfig.hrManager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.job.api.JobDto;
import de.talentmatch.support.WebMvcSecurityTestConfig;

@WebMvcTest(JobController.class)
@Import(WebMvcSecurityTestConfig.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @Test
    void listJobsReturnsJobsWithCounters() throws Exception {
        when(jobService.listJobs()).thenReturn(List.of(job()));

        mockMvc.perform(get("/api/jobs").with(hrManager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Data Engineer"))
                .andExpect(jsonPath("$[0].statusCounts.NEW").value(3))
                .andExpect(jsonPath("$[0].averageScore").value(64));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
    }

    @Test
    void forbidsApplicantRole() throws Exception {
        mockMvc.perform(get("/api/jobs").with(applicant())).andExpect(status().isForbidden());
    }

    @Test
    void unknownJobReturnsProblemDetail404() throws Exception {
        when(jobService.findJob(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/99").with(hrManager()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Nicht gefunden"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void createJobValidatesInput() throws Exception {
        mockMvc.perform(post("/api/jobs").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"requiredSkills\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.requiredSkills").exists());
        verify(jobService, never()).createJob(any());
    }

    @Test
    void createJobReturns201() throws Exception {
        when(jobService.createJob(any())).thenReturn(job());

        mockMvc.perform(post("/api/jobs").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Data Engineer","department":"Data","experienceLevel":"Mid",
                                 "location":"Hamburg","description":"","requiredSkills":["Python","SQL"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    private static JobDto job() {
        return new JobDto(3L, "Data Engineer", "Data", "Mid", "Hamburg · Hybrid", "Pipelines",
                List.of("Python", "SQL"), LocalDate.of(2026, 9, 21),
                Map.of(ApplicationStatus.NEW, 3L, ApplicationStatus.MATCHED, 7L), 10, 64);
    }
}
