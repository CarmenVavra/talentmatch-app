package de.talentmatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-End über die REST-API mit echter Security, echter (In-Memory-)Datenbank und Demo-Daten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TalentMatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${talentmatch.seed.hr-email}")
    private String hrEmail;

    @Value("${talentmatch.seed.hr-password}")
    private String hrPassword;

    @Value("${talentmatch.seed.applicant-email}")
    private String applicantEmail;

    @Value("${talentmatch.seed.applicant-password}")
    private String applicantPassword;

    @Test
    void dashboardShowsSeededJobsAndKpis() throws Exception {
        String token = login(hrEmail, hrPassword);

        mockMvc.perform(get("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].title").value("Senior Java Developer"))
                .andExpect(jsonPath("$[0].totalApplications").value(12))
                .andExpect(jsonPath("$[0].statusCounts.NEW").value(2));

        mockMvc.perform(get("/api/dashboard/kpis").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openJobs").value(6))
                .andExpect(jsonPath("$.totalApplications").value(greaterThanOrEqualTo(114)));
    }

    @Test
    void uploadCreatesNewApplicationAndAnalysisFailsCleanlyWithoutApiKey() throws Exception {
        String token = login(hrEmail, hrPassword);
        String jobs = mockMvc.perform(get("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        Integer jobId = JsonPath.read(jobs, "$[5].id");

        MockMultipartFile pdf = new MockMultipartFile("file", "Lebenslauf_Maria_Schulz.pdf", "application/pdf",
                pdf("Maria Schulz - maria.schulz@example.org - Java, Spring Boot, Kubernetes"));
        String created = mockMvc.perform(multipart("/api/jobs/{id}/applications", jobId).file(pdf)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.candidate.firstName").value("Maria"))
                .andExpect(jsonPath("$.overallScore").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Integer applicationId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/applications/{id}/resume", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/applications/{id}/analyze", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(containsString("OPENAI_API_KEY")));

        MockMultipartFile image = new MockMultipartFile("file", "foto.png", "image/png", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/jobs/{id}/applications", jobId).file(image)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusNotesAndChecklistArePersisted() throws Exception {
        String token = login(hrEmail, hrPassword);
        String all = mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(all, "$[?(@.candidate.lastName == 'Weber')].id");
        Integer id = ids.get(0);

        mockMvc.perform(patch("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/applications/{id}/notes", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Sehr gutes Gespräch\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/applications/{id}/questions/1", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"discussed\":true}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"))
                .andExpect(jsonPath("$.interviewNotes").value("Sehr gutes Gespräch"))
                .andExpect(jsonPath("$.analysis.questions[1].discussed").value(true))
                .andExpect(jsonPath("$.candidate.profile.positions[0].company").value("Siemens"));
    }

    @Test
    void applicantRoleHasNoAccessToHrEndpoints() throws Exception {
        String token = login(applicantEmail, applicantPassword);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("APPLICANT"));
        mockMvc.perform(get("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
    }

    @Test
    void applicantRegistersAppliesAndSeesOnlyTheStatus() throws Exception {
        String register = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Lea","lastName":"Neu","email":"lea.neu@example.org",
                                 "password":"bewerbung-2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("APPLICANT"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(register, "$.token");

        String jobs = mockMvc.perform(get("/api/applicant/jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].averageScore").doesNotExist())
                .andExpect(jsonPath("$[0].applied").value(false))
                .andReturn().getResponse().getContentAsString();
        Integer jobId = JsonPath.read(jobs, "$[1].id");

        MockMultipartFile pdf = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                pdf("Lea Neu - Angular, TypeScript, RxJS"));
        mockMvc.perform(multipart("/api/applicant/jobs/{id}/applications", jobId).file(pdf)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        mockMvc.perform(multipart("/api/applicant/jobs/{id}/applications", jobId).file(pdf)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/applicant/applications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$[0].overallScore").doesNotExist());

        // HR sieht die Portal-Bewerbung mit den Stammdaten aus dem Konto
        String hrToken = login(hrEmail, hrPassword);
        mockMvc.perform(get("/api/jobs/{id}/applications", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.candidate.lastName == 'Neu')].candidate.firstName").value("Lea"));
        mockMvc.perform(get("/api/applicant/jobs").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void seededApplicantHasDemoApplications() throws Exception {
        String token = login(applicantEmail, applicantPassword);
        mockMvc.perform(get("/api/applicant/applications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jobTitle").value("DevOps Engineer"))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$[1].status").value("INTERVIEW"));
    }

    @Test
    void registrationRejectsExistingEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"K\",\"lastName\":\"V\",\"email\":\"" + hrEmail
                                + "\",\"password\":\"irgendwas-123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void userCanChangePassword() throws Exception {
        String token = login(applicantEmail, applicantPassword);
        String newPassword = "ganz-neues-passwort";

        mockMvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicantPassword, newPassword)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(applicantEmail, applicantPassword)))
                .andExpect(status().isUnauthorized());

        String newToken = login(applicantEmail, newPassword);
        mockMvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, bearer(newToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newPassword, applicantPassword)))
                .andExpect(status().isNoContent());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        assertThat(token).isNotBlank();
        return token;
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String json(String current, String next) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static byte[] pdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
