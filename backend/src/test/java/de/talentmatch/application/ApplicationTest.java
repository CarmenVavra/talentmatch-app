package de.talentmatch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.talentmatch.auth.Role;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.job.JobPosition;

class ApplicationTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 17, 9, 0);

    @Test
    void portalApplicationIsUnreadUntilHrHasSeenIt() {
        Application application = portalApplication();
        assertThat(application.isUnread()).isTrue();

        application.markSeenByHr(CREATED.plusHours(1));

        assertThat(application.isUnread()).isFalse();
    }

    @Test
    void portalApplicationStaysUnreadAfterAutomaticAnalysis() {
        Application application = portalApplication();
        application.setStatus(ApplicationStatus.MATCHED);

        assertThat(application.isUnread()).isTrue();
    }

    @Test
    void processedPortalApplicationIsNotUnread() {
        Application application = portalApplication();
        application.setStatus(ApplicationStatus.INTERVIEW);

        assertThat(application.isUnread()).isFalse();
    }

    @Test
    void hrUploadsAreNeverUnread() {
        Application application = new Application(job(), new Candidate("Lena", "Hoffmann"), CREATED);

        assertThat(application.isUnread()).isFalse();
    }

    private static Application portalApplication() {
        Application application = new Application(job(), new Candidate("Max", "Mustermann"), CREATED);
        application.setApplicantAccount(
                new UserAccount("max@example.org", "hash", "Max", "Mustermann", null, Role.APPLICANT));
        return application;
    }

    private static JobPosition job() {
        return new JobPosition("Data Engineer", "Data", "Mid", "Hamburg", null, List.of("Python"),
                LocalDate.of(2026, 9, 21));
    }
}
