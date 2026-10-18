package de.talentmatch.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationDeletionService;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.job.JobPosition;
import de.talentmatch.mail.OutgoingMailRepository;
import de.talentmatch.talentpool.TalentPoolConsent;

@DataJpaTest
@ActiveProfiles("test")
class RetentionServiceTest {

    private static final LocalDateTime REJECTED = LocalDateTime.of(2026, 4, 1, 9, 0);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private OutgoingMailRepository mailRepository;

    @Autowired
    private TestEntityManager entityManager;

    private RetentionService service;
    private JobPosition job;

    @BeforeEach
    void setUp() {
        TalentMatchProperties properties = new TalentMatchProperties(null, null, null, null, null, null,
                new TalentMatchProperties.Privacy(Duration.ofDays(180)), null);
        service = new RetentionService(applicationRepository,
                new ApplicationDeletionService(applicationRepository, mailRepository), properties);
        job = entityManager.persist(new JobPosition("Data Engineer", "Data", "Mid", "Hamburg", null,
                List.of("Python"), LocalDate.of(2026, 3, 1)));
    }

    @Test
    void deletesOnlyRejectionsWithoutTalentPoolAfterTheRetentionPeriod() {
        Long expired = persist("Rene", application -> application.changeStatus(ApplicationStatus.REJECTED, REJECTED));
        Long recent = persist("Rita", application ->
                application.changeStatus(ApplicationStatus.REJECTED, REJECTED.plusDays(30)));
        Long pooled = persist("Petra", application -> {
            TalentPoolConsent consent = TalentPoolConsent.request("token-petra", REJECTED, REJECTED.plusDays(14));
            consent.confirm(REJECTED.plusDays(1), REJECTED.plusDays(730));
            application.requestTalentPool(consent);
        });
        Long interview = persist("Ines", application -> application.changeStatus(ApplicationStatus.INTERVIEW,
                REJECTED));

        int deleted = service.deleteExpiredRejections(REJECTED.plusDays(181));

        assertThat(deleted).isEqualTo(1);
        assertThat(applicationRepository.findById(expired)).isEmpty();
        assertThat(applicationRepository.findAllById(List.of(recent, pooled, interview))).hasSize(3);
    }

    @Test
    void rejectionsWithoutStartDateBeginTheirPeriodAtTheFirstRun() {
        Long legacy = persist("Lars", application -> application.setStatus(ApplicationStatus.REJECTED));
        LocalDateTime firstRun = LocalDateTime.of(2026, 10, 18, 9, 0);

        assertThat(service.deleteExpiredRejections(firstRun)).isZero();
        entityManager.clear();
        assertThat(applicationRepository.findById(legacy).orElseThrow().getRejectedAt()).contains(firstRun);

        assertThat(service.deleteExpiredRejections(firstRun.plusDays(181))).isEqualTo(1);
        assertThat(applicationRepository.findById(legacy)).isEmpty();
    }

    private Long persist(String firstName, Consumer<Application> setup) {
        Application application = new Application(job, new Candidate(firstName, "Test"), REJECTED.minusDays(10));
        setup.accept(application);
        Long id = entityManager.persistAndFlush(application).getId();
        entityManager.clear();
        return id;
    }
}
