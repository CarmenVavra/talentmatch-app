package de.talentmatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import de.talentmatch.common.persistence.SchemaUpgrade;
import de.talentmatch.job.JobPosition;
import de.talentmatch.talentpool.TalentPoolService;

/**
 * End-to-End über die REST-API mit echter Security, echter (In-Memory-)Datenbank und Demo-Daten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TalentMatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaUpgrade schemaUpgrade;

    @Autowired
    private TalentPoolService talentPoolService;

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
                                 "password":"bewerbung-2026","privacyAccepted":true}
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

        // HR sieht die Portal-Bewerbung mit den Stammdaten aus dem Konto – als „neu“, bis sie geöffnet wurde
        String hrToken = login(hrEmail, hrPassword);
        String hrList = mockMvc.perform(get("/api/jobs/{id}/applications", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.candidate.lastName == 'Neu')].candidate.firstName").value("Lea"))
                .andExpect(jsonPath("$[?(@.candidate.lastName == 'Neu')].unread").value(true))
                .andReturn().getResponse().getContentAsString();
        List<Integer> leaIds = JsonPath.read(hrList, "$[?(@.candidate.lastName == 'Neu')].id");
        mockMvc.perform(post("/api/applications/{id}/seen", leaIds.getFirst())
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/{id}", leaIds.getFirst())
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$.unread").value(false));
        mockMvc.perform(post("/api/applications/{id}/seen", leaIds.getFirst())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/applicant/jobs").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void applicantSubmitsSkillProfileAndCoverLetterWhichHrCanOpen() throws Exception {
        String register = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Tom","lastName":"Unterlagen","email":"tom.unterlagen@example.org",
                                 "password":"bewerbung-2026","privacyAccepted":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(register, "$.token");
        String jobs = mockMvc.perform(get("/api/applicant/jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        Integer jobId = JsonPath.read(jobs, "$[2].id");
        MockMultipartFile resume = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                pdf("Tom Unterlagen - Python, SQL"));
        MockMultipartFile skillProfile = new MockMultipartFile("skillProfile", "skills.pdf", "application/pdf",
                pdf("Skills: Python, Spark, Airflow"));
        MockMultipartFile coverLetter = new MockMultipartFile("coverLetter", "anschreiben.pdf", "application/pdf",
                pdf("Ich moechte Datenpipelines bauen."));

        // Ein ungültiges Zusatzdokument verhindert die gesamte Bewerbung
        MockMultipartFile wordFile = new MockMultipartFile("skillProfile", "skills.docx", "application/msword",
                new byte[] {1});
        mockMvc.perform(multipart("/api/applicant/jobs/{id}/applications", jobId).file(resume).file(wordFile)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Skill-Profil:")));

        mockMvc.perform(multipart("/api/applicant/jobs/{id}/applications", jobId)
                        .file(resume).file(skillProfile).file(coverLetter)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillProfileFileName").value("skills.pdf"))
                .andExpect(jsonPath("$.coverLetterFileName").value("anschreiben.pdf"));

        String hrToken = login(hrEmail, hrPassword);
        String list = mockMvc.perform(get("/api/jobs/{id}/applications", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(list, "$[?(@.candidate.lastName == 'Unterlagen')].id");
        assertThat(ids).hasSize(1);
        Integer applicationId = ids.getFirst();

        mockMvc.perform(get("/api/applications/{id}", applicationId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeFileName").value("cv.pdf"))
                .andExpect(jsonPath("$.skillProfileFileName").value("skills.pdf"))
                .andExpect(jsonPath("$.coverLetterFileName").value("anschreiben.pdf"));
        mockMvc.perform(get("/api/applications/{id}/skill-profile", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("skills.pdf")));
        mockMvc.perform(get("/api/applications/{id}/cover-letter", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"));
        mockMvc.perform(get("/api/applications/{id}/skill-profile", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentEndpointsReturn404WhenNothingWasSubmitted() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        String all = mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andReturn().getResponse().getContentAsString();
        Integer seededId = JsonPath.read(all, "$[-1].id");

        mockMvc.perform(get("/api/applications/{id}/cover-letter", seededId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void hrUpdatesAndDeletesJobIncludingItsApplications() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        String created = mockMvc.perform(post("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"QA Engineer","department":"Engineering","experienceLevel":"Mid",
                                 "location":"Berlin","description":"Tests","requiredSkills":["Selenium"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer jobId = JsonPath.read(created, "$.id");
        String createdDate = JsonPath.read(created, "$.createdDate");

        mockMvc.perform(put("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Senior QA Engineer","department":"Engineering","experienceLevel":"Senior",
                                 "location":"Berlin · Remote","description":"Testautomatisierung",
                                 "requiredSkills":[" Selenium ","Cypress","Cypress"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior QA Engineer"))
                .andExpect(jsonPath("$.requiredSkills.length()").value(2))
                .andExpect(jsonPath("$.createdDate").value(createdDate));

        // Bewerbung über das Portal (mit Zusatzunterlage), damit das Löschen auch Abhängigkeiten entfernt
        String register = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ida","lastName":"Loeschtest","email":"ida.loeschtest@example.org",
                                 "password":"bewerbung-2026","privacyAccepted":true}
                                """))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(register, "$.token");
        mockMvc.perform(get("/api/applicant/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.description").value("Testautomatisierung"));
        String application = mockMvc.perform(multipart("/api/applicant/jobs/{id}/applications", jobId)
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf("Ida - Selenium")))
                        .file(new MockMultipartFile("coverLetter", "brief.pdf", "application/pdf",
                                pdf("Motivation")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer applicationId = JsonPath.read(application, "$.id");

        mockMvc.perform(delete("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/applicant/applications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptsLongJobDescriptionsUpToTheLimit() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        String created = mockMvc.perform(post("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("**Ihre Aufgaben**\\n" + "x".repeat(15_000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer jobId = JsonPath.read(created, "$.id");
        String description = JsonPath.read(created, "$.description");
        assertThat(description).hasSize(15_000 + "**Ihre Aufgaben**\n".length());

        mockMvc.perform(put("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("x".repeat(20_001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").value(containsString("20.000")));

        mockMvc.perform(delete("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void schemaUpgradeWidensAnOldDescriptionColumn() {
        jdbcTemplate.execute("alter table job_position alter column description set data type varchar(4000)");

        schemaUpgrade.run(null);
        schemaUpgrade.run(null);

        Integer length = jdbcTemplate.queryForObject("select character_maximum_length from information_schema.columns"
                + " where lower(table_name) = 'job_position' and lower(column_name) = 'description'", Integer.class);
        assertThat(length).isEqualTo(JobPosition.MAX_DESCRIPTION_LENGTH);
    }

    @Test
    void hrUploadGroupsDocumentsOfOnePersonAndMergesDuplicates() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        String created = mockMvc.perform(post("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Frontend")))
                .andReturn().getResponse().getContentAsString();
        Integer jobId = JsonPath.read(created, "$.id");

        // 1. Vorabprüfung: Lebenslauf + Skill-Profil von Carmen (gleiche E-Mail) und ein Lebenslauf von Max
        mockMvc.perform(multipart("/api/jobs/{id}/applications/inspect", jobId)
                        .file(pdfPart("files", "Lebenslauf_Carmen_Gregor.pdf",
                                "Carmen Gregor carmen@example.org Berufserfahrung Ausbildung"))
                        .file(pdfPart("files", "Skillprofil_Carmen_Gregor.pdf",
                                "Skills carmen@example.org Angular, Java"))
                        .file(pdfPart("files", "CV_Max_Muster.pdf", "Max Muster max@example.org Berufserfahrung"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[1].type").value("SKILL_PROFILE"))
                .andExpect(jsonPath("$.groups.length()").value(2))
                .andExpect(jsonPath("$.groups[0].fileIndexes.length()").value(2))
                .andExpect(jsonPath("$.groups[0].match").value("EMAIL"))
                .andExpect(jsonPath("$.groups[0].needsReview").value(false));

        // 2. Upload einer Gruppe: eine Bewerbung mit Lebenslauf und Skill-Profil
        mockMvc.perform(multipart("/api/jobs/{id}/applications", jobId)
                        .file(pdfPart("file", "Lebenslauf_Max_Muster.pdf", "Max Muster Berufserfahrung"))
                        .file(pdfPart("skillProfile", "Skills_Max.pdf", "Skills Java"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isCreated());

        // 3. Der bisherige Fehlerfall: Skill-Profil einzeln hochgeladen → zwei Bewerbungen derselben Person
        String first = mockMvc.perform(multipart("/api/jobs/{id}/applications", jobId)
                        .file(pdfPart("file", "Lebenslauf_Carmen_Gregor.pdf", "Carmen Gregor Wien Berufserfahrung"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(multipart("/api/jobs/{id}/applications", jobId)
                        .file(pdfPart("file", "Skillprofil_Carmen_Gregor.pdf", "Skills Angular Java Material"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andReturn().getResponse().getContentAsString();
        Integer targetId = JsonPath.read(first, "$.id");
        Integer sourceId = JsonPath.read(second, "$.id");

        mockMvc.perform(get("/api/applications/{id}/duplicates", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sourceId))
                .andExpect(jsonPath("$[0].reason").value("NAME"))
                .andExpect(jsonPath("$[0].documents[0].suggestedAs").value("SKILL_PROFILE"));

        mockMvc.perform(post("/api/applications/{id}/merge", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceId\":" + sourceId
                                + ",\"documents\":[{\"from\":\"RESUME\",\"to\":\"SKILL_PROFILE\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeFileName").value("Lebenslauf_Carmen_Gregor.pdf"))
                .andExpect(jsonPath("$.skillProfileFileName").value("Skillprofil_Carmen_Gregor.pdf"))
                .andExpect(jsonPath("$.analysis").doesNotExist());

        mockMvc.perform(get("/api/applications/{id}", sourceId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/applications/{id}/skill-profile", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/applications/{id}/duplicates", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(delete("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void mergeIsNeverPossibleAcrossJobs() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobA = createJob(hrToken);
        Integer jobB = createJob(hrToken);
        Integer onA = hrUpload(hrToken, jobA, "Lebenslauf_Ella_Kreuz.pdf", "Ella Kreuz ella@example.org");
        Integer onB = hrUpload(hrToken, jobB, "Skillprofil_Ella_Kreuz.pdf", "Skills ella@example.org");

        // Gleiche Person, aber andere Stelle: kein Duplikat-Hinweis …
        mockMvc.perform(get("/api/applications/{id}/duplicates", onA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$.length()").value(0));
        // … und auch direkt per API nicht zusammenführbar
        mockMvc.perform(post("/api/applications/{id}/merge", onA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeJson(onB, "RESUME", "SKILL_PROFILE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("dieselbe Stelle")));
        mockMvc.perform(get("/api/applications/{id}", onB).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/applications/{id}", onA).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$.skillProfileFileName").doesNotExist());

        deleteJob(hrToken, jobA);
        deleteJob(hrToken, jobB);
    }

    @Test
    void mergeIsNeverPossibleAcrossDifferentApplicantAccounts() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        // Zwei verschiedene Personen mit gleichem Namen bewerben sich über das Portal
        Integer first = portalApply(register("anna.berg.1@example.org"), jobId);
        Integer second = portalApply(register("anna.berg.2@example.org"), jobId);

        mockMvc.perform(get("/api/applications/{id}/duplicates", first)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/applications/{id}/merge", first)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeJson(second, "RESUME", "SKILL_PROFILE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("verschiedenen Bewerber-Konten")));

        deleteJob(hrToken, jobId);
    }

    @Test
    void portalAndHrUploadOfTheSamePersonOnTheSameJobCanBeMerged() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        String applicantToken = register("ben.gleich@example.org");
        Integer portal = portalApply(applicantToken, jobId);
        Integer hr = hrUpload(hrToken, jobId, "Skillprofil_Ben_Berg.pdf", "Skills ben.gleich@example.org");

        mockMvc.perform(get("/api/applications/{id}/duplicates", hr)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$[0].id").value(portal))
                .andExpect(jsonPath("$[0].reason").value("EMAIL"));
        mockMvc.perform(post("/api/applications/{id}/merge", portal)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeJson(hr, "RESUME", "SKILL_PROFILE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillProfileFileName").value("Skillprofil_Ben_Berg.pdf"));

        // Die Bewerber:in sieht weiterhin genau ihre eine Bewerbung
        mockMvc.perform(get("/api/applicant/applications").header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].skillProfileFileName").value("Skillprofil_Ben_Berg.pdf"));

        deleteJob(hrToken, jobId);
    }

    @Test
    void talentPoolRequestConfirmationAndExpiry() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        Integer id = hrUpload(hrToken, jobId, "Lebenslauf_Lina_Pool.pdf", "Lina Pool lina.pool@example.org");

        // Vorschlag: Empfänger:in, Betreff und bearbeitbarer Text mit Platzhalter für die Links
        mockMvc.perform(get("/api/applications/{id}/talent-pool/preview", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipient").value("lina.pool@example.org"))
                .andExpect(jsonPath("$.body").value(containsString("[Antwort-Links]")))
                .andExpect(jsonPath("$.body").value(containsString("Katrin Vogel")));

        requestTalentPool(hrToken, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.talentPool.status").value("REQUESTED"));
        requestTalentPool(hrToken, id).andExpect(status().isConflict());

        // Die E-Mail liegt im Postausgang (ohne Mailserver: nicht versendet) und enthält den persönlichen Link
        String token = talentPoolToken(hrToken, id);
        mockMvc.perform(get("/api/public/talent-pool/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Lina"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(post("/api/public/talent-pool/{token}/confirm", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.expiresAt").exists());
        mockMvc.perform(get("/api/talent-pool").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(jsonPath("$[?(@.applicationId == " + id + ")].talentPool.status").value("CONFIRMED"));
        assertThat(outboxSubjects(hrToken, id)).hasSize(2)
                .anySatisfy(subject -> assertThat(subject).contains("bleiben in Evidenz"));

        // Nach Ablauf der Aufbewahrungsdauer wird die Bewerbung samt E-Mails gelöscht
        talentPoolService.runMaintenance(LocalDateTime.now().plusDays(731));
        mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        assertThat(outboxSubjects(hrToken, id)).isEmpty();

        deleteJob(hrToken, jobId);
    }

    @Test
    void unansweredTalentPoolRequestsAreRemindedAndDeleted() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        Integer id = hrUpload(hrToken, jobId, "Lebenslauf_Otto_Still.pdf", "Otto Still otto.still@example.org");
        requestTalentPool(hrToken, id).andExpect(status().isOk());

        talentPoolService.runMaintenance(LocalDateTime.now().plusDays(8));
        talentPoolService.runMaintenance(LocalDateTime.now().plusDays(9));
        assertThat(outboxSubjects(hrToken, id)).hasSize(2)
                .anySatisfy(subject -> assertThat(subject).startsWith("Erinnerung"));

        talentPoolService.runMaintenance(LocalDateTime.now().plusDays(15));
        mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());

        deleteJob(hrToken, jobId);
    }

    @Test
    void declineDeletesImmediatelyAndInvalidatesTheLink() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        Integer id = hrUpload(hrToken, jobId, "Lebenslauf_Nora_Nein.pdf", "Nora Nein nora.nein@example.org");
        requestTalentPool(hrToken, id).andExpect(status().isOk());
        String token = talentPoolToken(hrToken, id);

        mockMvc.perform(post("/api/public/talent-pool/{token}/decline", token)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/talent-pool/{token}", token)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/public/talent-pool/{token}/confirm", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("nicht mehr gültig")));

        deleteJob(hrToken, jobId);
    }

    @Test
    void talentPoolRulesAndDeletingApplications() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);

        // Ohne E-Mail-Adresse kann nicht angefragt werden
        Integer withoutEmail = hrUpload(hrToken, jobId, "Lebenslauf_Ohne_Mail.pdf", "Ohne Mail Berufserfahrung");
        requestTalentPool(hrToken, withoutEmail)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("keine E-Mail-Adresse")));

        // Wird die Absage zurückgenommen, ist die Anfrage hinfällig
        Integer revived = hrUpload(hrToken, jobId, "Lebenslauf_Rita_Zurueck.pdf", "Rita rita.z@example.org");
        requestTalentPool(hrToken, revived).andExpect(status().isOk());
        String token = talentPoolToken(hrToken, revived);
        mockMvc.perform(patch("/api/applications/{id}", revived).header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.talentPool").doesNotExist());
        mockMvc.perform(get("/api/public/talent-pool/{token}", token)).andExpect(status().isNotFound());

        // HR löscht eine Bewerbung endgültig
        mockMvc.perform(delete("/api/applications/{id}", revived).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/{id}", revived).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        assertThat(outboxSubjects(hrToken, revived)).isEmpty();

        // Evidenz und Postausgang sind HR vorbehalten
        String applicantToken = login(applicantEmail, applicantPassword);
        mockMvc.perform(get("/api/talent-pool").header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/outbox").header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
                .andExpect(status().isForbidden());

        deleteJob(hrToken, jobId);
    }

    @Test
    void portalApplicantSeesTheTalentPoolRequest() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        String applicantToken = register("paula.portal@example.org");
        Integer id = portalApply(applicantToken, jobId);
        requestTalentPool(hrToken, id).andExpect(status().isOk());

        mockMvc.perform(get("/api/applicant/applications").header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].talentPoolStatus").value("REQUESTED"))
                .andExpect(jsonPath("$[0].talentPoolToken").value(talentPoolToken(hrToken, id)))
                .andExpect(jsonPath("$[0].talentPoolDeadline").exists());

        deleteJob(hrToken, jobId);
    }

    @Test
    void portalApplicantsAreNotifiedAboutEveryVisibleStatusChange() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        Integer id = portalApply(register("nora.notify@example.org"), jobId);

        // Eingangsbestätigung an das Portal-Konto, mit Link ins Portal
        String outbox = mockMvc.perform(get("/api/outbox").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andReturn().getResponse().getContentAsString();
        List<String> recipients = JsonPath.read(outbox, "$.mails[?(@.applicationId == " + id + ")].recipient");
        List<String> bodies = JsonPath.read(outbox, "$.mails[?(@.applicationId == " + id + ")].body");
        assertThat(recipients).containsExactly("nora.notify@example.org");
        assertThat(bodies.getFirst()).contains("Guten Tag Anna Berg", "/portal/applications");
        assertThat(outboxSubjects(hrToken, id))
                .containsExactly("Ihre Bewerbung als Lange Ausschreibung ist eingegangen");

        // „KI-analysiert“ ist intern und löst keine Mail aus, jeder sichtbare Wechsel dagegen schon
        changeStatus(hrToken, id, "MATCHED");
        assertThat(outboxSubjects(hrToken, id)).hasSize(1);
        changeStatus(hrToken, id, "INTERVIEW");
        changeStatus(hrToken, id, "INTERVIEW");
        changeStatus(hrToken, id, "HIRED");
        changeStatus(hrToken, id, "REJECTED");
        changeStatus(hrToken, id, "NEW");
        assertThat(outboxSubjects(hrToken, id)).containsExactly(
                "Ihre Bewerbung als Lange Ausschreibung wird geprüft",
                "Ihre Bewerbung als Lange Ausschreibung",
                "Zusage: Ihre Bewerbung als Lange Ausschreibung",
                "Einladung zum Kennenlernen: Lange Ausschreibung",
                "Ihre Bewerbung als Lange Ausschreibung ist eingegangen");

        // Von HR hochgeladene Bewerbungen ohne Portal-Konto erhalten keine Mails
        Integer uploaded = hrUpload(hrToken, jobId, "Lebenslauf_Olaf_Konto.pdf", "Olaf Konto olaf@example.org");
        changeStatus(hrToken, uploaded, "INTERVIEW");
        assertThat(outboxSubjects(hrToken, uploaded)).isEmpty();

        deleteJob(hrToken, jobId);
    }

    @Test
    void talentPoolRequestSendsNoAdditionalRejectionMail() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        Integer id = portalApply(register("rita.reject@example.org"), jobId);
        requestTalentPool(hrToken, id).andExpect(status().isOk());

        assertThat(outboxSubjects(hrToken, id)).containsExactly("Ihre Bewerbung",
                "Ihre Bewerbung als Lange Ausschreibung ist eingegangen");

        deleteJob(hrToken, jobId);
    }

    @Test
    void applicantWithdrawsOnlyOwnApplications() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        String ownerToken = register("wanda.withdraw@example.org");
        Integer id = portalApply(ownerToken, jobId);
        String otherToken = register("otto.other@example.org");

        mockMvc.perform(delete("/api/applicant/applications/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/applicant/applications/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applicant/applications").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        assertThat(outboxSubjects(hrToken, id)).isEmpty();

        deleteJob(hrToken, jobId);
    }

    @Test
    void applicantDeletesAccountWithAllApplications() throws Exception {
        String hrToken = login(hrEmail, hrPassword);
        Integer jobId = createJob(hrToken);
        String token = register("dora.delete@example.org");
        Integer id = portalApply(token, jobId);

        mockMvc.perform(post("/api/applicant/account/delete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"falsch-123\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/applicant/account/delete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"bewerbung-2026\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("dora.delete@example.org", "bewerbung-2026")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNotFound());
        assertThat(outboxSubjects(hrToken, id)).isEmpty();
        mockMvc.perform(post("/api/applicant/account/delete").header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"x\"}"))
                .andExpect(status().isForbidden());

        deleteJob(hrToken, jobId);
    }

    @Test
    void registrationRequiresThePrivacyNotice() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Pia","lastName":"Privat","email":"pia@example.org",
                                 "password":"bewerbung-2026","privacyAccepted":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.privacyAccepted").exists());
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
                                + "\",\"password\":\"irgendwas-123\",\"privacyAccepted\":true}"))
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

    private ResultActions requestTalentPool(String hrToken, Integer id) throws Exception {
        return mockMvc.perform(post("/api/applications/{id}/talent-pool", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Ihre Bewerbung\",\"body\":\"Guten Tag,\\n\\n[Antwort-Links]\\n\\nGruß\"}"));
    }

    /** Liest den persönlichen Antwort-Link aus der ersten Evidenz-E-Mail der Bewerbung im Postausgang. */
    private String talentPoolToken(String hrToken, Integer applicationId) throws Exception {
        String outbox = mockMvc.perform(get("/api/outbox").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryEnabled").value(false))
                .andReturn().getResponse().getContentAsString();
        List<String> bodies = JsonPath.read(outbox, "$.mails[?(@.applicationId == " + applicationId + ")].body");
        Pattern link = Pattern.compile("/evidenz/([A-Za-z0-9_-]+)\\?antwort=ja");
        return bodies.reversed().stream()
                .map(link::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Keine Evidenz-Mail im Postausgang"));
    }

    private void changeStatus(String hrToken, Integer id, String status) throws Exception {
        mockMvc.perform(patch("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private List<String> outboxSubjects(String hrToken, Integer applicationId) throws Exception {
        String outbox = mockMvc.perform(get("/api/outbox").header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(outbox, "$.mails[?(@.applicationId == " + applicationId + ")].subject");
    }

    private Integer createJob(String hrToken) throws Exception {
        String created = mockMvc.perform(post("/api/jobs").header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Test")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private void deleteJob(String hrToken, Integer jobId) throws Exception {
        mockMvc.perform(delete("/api/jobs/{id}", jobId).header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isNoContent());
    }

    private Integer hrUpload(String hrToken, Integer jobId, String fileName, String text) throws Exception {
        String created = mockMvc.perform(multipart("/api/jobs/{id}/applications", jobId)
                        .file(pdfPart("file", fileName, text))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    /** Registriert eine Bewerber:in „Anna Berg“ bzw. mit der angegebenen E-Mail und liefert das Token. */
    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Anna","lastName":"Berg","email":"%s","password":"bewerbung-2026",
                                 "privacyAccepted":true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private Integer portalApply(String applicantToken, Integer jobId) throws Exception {
        String response = mockMvc.perform(multipart("/api/applicant/jobs/{id}/applications", jobId)
                        .file(pdfPart("file", "cv.pdf", "Anna Berg Berufserfahrung"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private static String mergeJson(Integer sourceId, String from, String to) {
        return "{\"sourceId\":" + sourceId + ",\"documents\":[{\"from\":\"" + from + "\",\"to\":\"" + to
                + "\"}]}";
    }

    private static MockMultipartFile pdfPart(String part, String fileName, String text) throws IOException {
        return new MockMultipartFile(part, fileName, "application/pdf", pdf(text));
    }

    private static String jobJson(String description) {
        return """
                {"title":"Lange Ausschreibung","department":"Engineering","experienceLevel":"Mid",
                 "location":"Berlin","description":"%s","requiredSkills":["Java"]}
                """.formatted(description);
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
