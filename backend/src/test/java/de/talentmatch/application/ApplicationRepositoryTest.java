package de.talentmatch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import de.talentmatch.analysis.AnalysisResult;
import de.talentmatch.analysis.InterviewQuestion;
import de.talentmatch.application.ApplicationRepository.JobAverageScore;
import de.talentmatch.application.ApplicationRepository.JobStatusCount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.job.JobPosition;

@DataJpaTest
@ActiveProfiles("test")
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private JobPosition job;

    @BeforeEach
    void setUp() {
        job = entityManager.persist(new JobPosition("Data Engineer", "Data", "Mid", "Hamburg", null,
                List.of("Python", "SQL"), LocalDate.of(2026, 9, 21)));
        persist("Anna", ApplicationStatus.MATCHED, 80, LocalDateTime.of(2026, 10, 1, 9, 0));
        persist("Ben", ApplicationStatus.MATCHED, 61, LocalDateTime.of(2026, 10, 3, 9, 0));
        persist("Cem", ApplicationStatus.NEW, null, LocalDateTime.of(2026, 10, 5, 9, 0));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void loadsApplicationsOfJobWithCandidateAndAnalysisNewestFirst() {
        List<Application> result = repository.findByJobPositionIdOrderByCreatedAtDesc(job.getId());
        entityManager.clear();

        assertThat(result).extracting(a -> a.getCandidate().getFirstName()).containsExactly("Cem", "Ben", "Anna");
        assertThat(result.get(1).getAnalysis()).hasValueSatisfying(r -> assertThat(r.getOverallScore()).isEqualTo(61));
    }

    @Test
    void aggregatesCountsPerStatusAndAverageScore() {
        List<JobStatusCount> counts = repository.countByJobAndStatus();
        List<JobAverageScore> averages = repository.averageScoreByJob();

        assertThat(counts).extracting(JobStatusCount::getStatus, JobStatusCount::getTotal)
                .containsExactlyInAnyOrder(tuple(ApplicationStatus.MATCHED, 2L), tuple(ApplicationStatus.NEW, 1L));
        assertThat(averages).singleElement().satisfies(avg -> assertThat(avg.getAverageScore()).isEqualTo(70.5));
    }

    @Test
    void countsByStatusAndCreationDate() {
        assertThat(repository.countByStatus(ApplicationStatus.NEW)).isEqualTo(1);
        assertThat(repository.countByCreatedAtGreaterThanEqual(LocalDateTime.of(2026, 10, 3, 0, 0))).isEqualTo(2);
    }

    @Test
    void persistsJsonColumnsRoundTrip() {
        Application loaded = repository.findWithDetailsById(
                repository.findByJobPositionIdOrderByCreatedAtDesc(job.getId()).get(2).getId()).orElseThrow();

        AnalysisResult analysis = loaded.getAnalysis().orElseThrow();
        assertThat(analysis.getMatchingSkills()).containsExactly("Python");
        assertThat(analysis.getSuggestedQuestions()).containsExactly(new InterviewQuestion("Technik", "Frage?", true));
        assertThat(loaded.getJobPosition().getRequiredSkills()).containsExactly("Python", "SQL");
    }

    private void persist(String firstName, ApplicationStatus status, Integer score, LocalDateTime createdAt) {
        Application application = new Application(job, new Candidate(firstName, "Test"), createdAt);
        if (score != null) {
            application.applyAnalysis(new AnalysisResult(new AnalysisResult.Scores(score, score, score, score, score),
                    "Zusammenfassung", List.of("Python"), List.of("SQL"), List.of(),
                    List.of(new InterviewQuestion("Technik", "Frage?", true)), "gpt-4o", Instant.EPOCH));
        }
        application.setStatus(status);
        entityManager.persist(application);
    }
}
