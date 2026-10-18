package de.talentmatch.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApplicantMailsTest {

    private static final String PORTAL = "https://talentmatch.example.com/portal/applications";

    @Test
    void receivedMailThanksAndLinksToThePortal() {
        ApplicantMails.Mail mail = ApplicantMails.received("Lea", "Neu", "Data Engineer", PORTAL);

        assertThat(mail.subject()).isEqualTo("Ihre Bewerbung als Data Engineer ist eingegangen");
        assertThat(mail.body()).startsWith("Guten Tag Lea Neu,")
                .contains("„Data Engineer“", "eingegangen", PORTAL)
                .endsWith("Ihr Recruiting-Team");
    }

    @ParameterizedTest
    @EnumSource(ApplicantStatus.class)
    void everyVisibleStatusHasAPersonalMail(ApplicantStatus status) {
        ApplicantMails.Mail mail = ApplicantMails.statusChanged(status, "Lea", "Neu", "Data Engineer", PORTAL);

        assertThat(mail.subject()).contains("Data Engineer");
        assertThat(mail.body()).startsWith("Guten Tag Lea Neu,")
                .contains("„Data Engineer“", PORTAL)
                .doesNotContain("%s");
    }

    @Test
    void subjectsNameTheDecision() {
        assertThat(ApplicantMails.statusChanged(ApplicantStatus.INTERVIEW, "A", "B", "Job", PORTAL).subject())
                .startsWith("Einladung zum Kennenlernen");
        assertThat(ApplicantMails.statusChanged(ApplicantStatus.OFFER, "A", "B", "Job", PORTAL).subject())
                .startsWith("Zusage");
        assertThat(ApplicantMails.statusChanged(ApplicantStatus.REJECTED, "A", "B", "Job", PORTAL).body())
                .contains("für eine andere Bewerbung entschieden");
    }
}
