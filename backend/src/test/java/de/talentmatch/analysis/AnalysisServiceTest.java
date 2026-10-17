package de.talentmatch.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.talentmatch.ai.MatchAssessment;
import de.talentmatch.ai.QuestionSuggestion;
import de.talentmatch.ai.RecruitingAiService;
import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.candidate.CandidateProfile;
import de.talentmatch.common.exception.AiAnalysisException;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.job.JobPosition;

class AnalysisServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T10:00:00Z");

    private ApplicationRepository repository;
    private RecruitingAiService aiService;
    private AnalysisService service;
    private Application application;

    @BeforeEach
    void setUp() {
        repository = mock(ApplicationRepository.class);
        aiService = mock(RecruitingAiService.class);
        when(aiService.modelName()).thenReturn("gpt-4o");
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new AnalysisService(repository, aiService, new SkillMatcher(), transactionTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC));

        JobPosition job = new JobPosition("Senior Java Developer", "Engineering", "Senior", "München", "",
                List.of("Java", "Spring Boot", "Kubernetes", "AWS"), LocalDate.of(2026, 10, 2));
        Candidate candidate = new Candidate("Neu", "Bewerber:in");
        candidate.setRawResumeText("Lena Hoffmann – Java, Spring Boot 3, Kubernetes");
        application = new Application(job, candidate, LocalDateTime.of(2026, 10, 16, 9, 0));
        when(repository.findWithDetailsById(1L)).thenReturn(Optional.of(application));
    }

    @Test
    void analyzeParsesResumeCombinesScoresAndMovesNewApplicationToMatched() {
        when(aiService.parseResume(anyString())).thenReturn(profile());
        when(aiService.assessMatch(any(), any(), any())).thenReturn(new MatchAssessment(80, 90, 70,
                "Starke Passung.", List.of("Kündigungsfrist 3 Monate"),
                List.of(new QuestionSuggestion("Fehlender Skill", "Wie gut kennen Sie AWS?"))));

        ApplicationDetailDto detail = service.analyze(1L);

        // Skills: 3 von 4 = 75 → 0.5*75 + 0.3*80 + 0.1*90 + 0.1*70 = 77.5 → 78
        assertThat(detail.analysis().overallScore()).isEqualTo(78);
        assertThat(detail.analysis().skillsScore()).isEqualTo(75);
        assertThat(detail.analysis().matchingSkills()).containsExactly("Java", "Spring Boot", "Kubernetes");
        assertThat(detail.analysis().missingSkills()).containsExactly("AWS");
        assertThat(detail.analysis().questions()).singleElement()
                .satisfies(q -> assertThat(q.discussed()).isFalse());
        assertThat(detail.analysis().analyzedAt()).isEqualTo(NOW);
        assertThat(detail.status()).isEqualTo(ApplicationStatus.MATCHED);
        assertThat(detail.candidate().firstName()).isEqualTo("Lena");
        assertThat(detail.candidate().profile()).isNotNull();
    }

    @Test
    void analyzeKeepsStatusOfApplicationsBeyondNew() {
        application.setStatus(ApplicationStatus.INTERVIEW);
        when(aiService.parseResume(anyString())).thenReturn(profile());
        when(aiService.assessMatch(any(), any(), any()))
                .thenReturn(new MatchAssessment(50, 50, 50, "Ok.", List.of(), List.of()));

        assertThat(service.analyze(1L).status()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void analyzePropagatesAiFailureWithoutChangingApplication() {
        when(aiService.parseResume(anyString())).thenThrow(new AiAnalysisException("kaputt"));

        assertThatThrownBy(() -> service.analyze(1L)).isInstanceOf(AiAnalysisException.class);
        assertThat(application.getAnalysis()).isEmpty();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.NEW);
    }

    @Test
    void generateMoreQuestionsRequiresExistingAnalysis() {
        assertThatThrownBy(() -> service.generateMoreQuestions(1L)).isInstanceOf(BusinessRuleException.class);
        verify(aiService, never()).generateMoreQuestions(any(), any(), anyList());
    }

    @Test
    void generateMoreQuestionsAppendsToExistingQuestions() {
        when(aiService.parseResume(anyString())).thenReturn(profile());
        when(aiService.assessMatch(any(), any(), any())).thenReturn(new MatchAssessment(60, 60, 60, "Ok.",
                List.of(), List.of(new QuestionSuggestion("Technik", "Frage 1"))));
        service.analyze(1L);
        when(aiService.generateMoreQuestions(any(), any(), anyList()))
                .thenReturn(List.of(new QuestionSuggestion("Werdegang", "Frage 2")));

        ApplicationDetailDto detail = service.generateMoreQuestions(1L);

        assertThat(detail.analysis().questions()).extracting(ApplicationDetailDto.InterviewQuestionDto::text)
                .containsExactly("Frage 1", "Frage 2");
    }

    private static CandidateProfile profile() {
        return new CandidateProfile("Lena", "Hoffmann", "lena@example.org", null, "München",
                "Senior Backend Engineer", "FinCore", List.of(), List.of("Java 17", "Spring Boot 3", "K8s"),
                "M.Sc. Informatik", List.of("Deutsch", "Englisch (C1)"), 7);
    }
}
