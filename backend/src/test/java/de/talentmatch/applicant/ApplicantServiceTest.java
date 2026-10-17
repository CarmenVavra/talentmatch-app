package de.talentmatch.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import de.talentmatch.applicant.api.MyApplicationDto;
import de.talentmatch.applicant.api.OpenJobDto;
import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.auth.Role;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.auth.UserAccountRepository;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.JobPosition;
import de.talentmatch.job.JobPositionRepository;
import de.talentmatch.resume.ApplicantDocuments;
import de.talentmatch.resume.ResumeUploadService;

class ApplicantServiceTest {

    private static final String EMAIL = "max@example.org";

    private ApplicationRepository applicationRepository;
    private JobPositionRepository jobRepository;
    private ResumeUploadService uploadService;
    private ApplicationEventPublisher events;
    private ApplicantService service;
    private UserAccount account;
    private JobPosition job;

    @BeforeEach
    void setUp() {
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        jobRepository = mock(JobPositionRepository.class);
        uploadService = mock(ResumeUploadService.class);
        events = mock(ApplicationEventPublisher.class);
        service = new ApplicantService(userRepository, jobRepository, applicationRepository, uploadService, events);

        account = new UserAccount(EMAIL, "hash", "Max", "Mustermann", null, Role.APPLICANT);
        ReflectionTestUtils.setField(account, "id", 5L);
        job = new JobPosition("Data Engineer", "Data", "Mid", "Hamburg", "Pipelines", List.of("Python"),
                LocalDate.of(2026, 9, 21));
        ReflectionTestUtils.setField(job, "id", 3L);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));
        when(jobRepository.findById(3L)).thenReturn(Optional.of(job));
    }

    @Test
    void listsOpenJobsAndMarksAlreadyAppliedOnes() {
        JobPosition other = new JobPosition("DevOps Engineer", "Engineering", "Mid", "Remote", null,
                List.of("Docker"), LocalDate.of(2026, 9, 9));
        ReflectionTestUtils.setField(other, "id", 4L);
        when(jobRepository.findAllByOrderByCreatedDateDescIdAsc()).thenReturn(List.of(job, other));
        when(applicationRepository.findJobIdsByApplicantAccountId(5L)).thenReturn(List.of(3L));

        List<OpenJobDto> jobs = service.listOpenJobs(EMAIL);

        assertThat(jobs).extracting(OpenJobDto::title, OpenJobDto::applied)
                .containsExactly(tuple("Data Engineer", true), tuple("DevOps Engineer", false));
    }

    @Test
    void findsSingleOpenJobWithAppliedFlag() {
        when(applicationRepository.existsByJobPositionIdAndApplicantAccountId(3L, 5L)).thenReturn(true);

        assertThat(service.findOpenJob(EMAIL, 3L)).hasValueSatisfying(dto -> {
            assertThat(dto.title()).isEqualTo("Data Engineer");
            assertThat(dto.description()).isEqualTo("Pipelines");
            assertThat(dto.applied()).isTrue();
        });
    }

    @Test
    void unknownOpenJobIsEmpty() {
        assertThat(service.findOpenJob(EMAIL, 99L)).isEmpty();
    }

    @Test
    void mapsInternalStatusesToApplicantStatuses() {
        Application matched = application(ApplicationStatus.MATCHED);
        Application hired = application(ApplicationStatus.HIRED);
        when(applicationRepository.findByApplicantAccountIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(matched, hired));

        assertThat(service.listMyApplications(EMAIL)).extracting(MyApplicationDto::status)
                .containsExactly(ApplicantStatus.RECEIVED, ApplicantStatus.OFFER);
    }

    @Test
    void applyStoresApplicationAndTriggersAutomaticAnalysis() {
        Application created = application(ApplicationStatus.NEW);
        ReflectionTestUtils.setField(created, "id", 77L);
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {1});
        ApplicantDocuments documents = ApplicantDocuments.resumeOnly(file);
        when(uploadService.submitForApplicant(job, account, documents)).thenReturn(created);

        MyApplicationDto result = service.apply(EMAIL, 3L, documents);

        assertThat(result.status()).isEqualTo(ApplicantStatus.RECEIVED);
        assertThat(result.jobTitle()).isEqualTo("Data Engineer");
        verify(events).publishEvent(new ApplicationSubmittedEvent(77L));
    }

    @Test
    void applyRejectsDuplicateApplications() {
        when(applicationRepository.existsByJobPositionIdAndApplicantAccountId(3L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(EMAIL, 3L, null)).isInstanceOf(BusinessRuleException.class);
        verify(uploadService, never()).submitForApplicant(any(), any(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void applyToUnknownJobFails() {
        assertThatThrownBy(() -> service.apply(EMAIL, 99L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void statusMappingHidesInternalSteps() {
        assertThat(ApplicantStatus.from(ApplicationStatus.NEW)).isEqualTo(ApplicantStatus.RECEIVED);
        assertThat(ApplicantStatus.from(ApplicationStatus.MATCHED)).isEqualTo(ApplicantStatus.RECEIVED);
        assertThat(ApplicantStatus.from(ApplicationStatus.INTERVIEW)).isEqualTo(ApplicantStatus.INTERVIEW);
        assertThat(ApplicantStatus.from(ApplicationStatus.REJECTED)).isEqualTo(ApplicantStatus.REJECTED);
    }

    private Application application(ApplicationStatus status) {
        Application application = new Application(job, new Candidate("Max", "Mustermann"),
                LocalDateTime.of(2026, 10, 10, 9, 0));
        application.setStatus(status);
        return application;
    }
}
