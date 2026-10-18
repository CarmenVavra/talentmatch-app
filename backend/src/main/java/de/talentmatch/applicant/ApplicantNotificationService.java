package de.talentmatch.applicant;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.ApplicationStatusChangedEvent;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.mail.MailService;

/**
 * Benachrichtigt Bewerber:innen mit Portal-Konto per E-Mail: Eingangsbestätigung und jede für sie sichtbare
 * Statusänderung. Von HR hochgeladene Bewerbungen ohne Konto erhalten keine Mails. Die Listener laufen in der
 * auslösenden Transaktion – der Versand startet erst nach dem Commit.
 */
@Service
public class ApplicantNotificationService {

    static final String PORTAL_PATH = "/portal/applications";

    private final ApplicationRepository applicationRepository;
    private final MailService mailService;
    private final TalentMatchProperties properties;

    public ApplicantNotificationService(ApplicationRepository applicationRepository, MailService mailService,
                                        TalentMatchProperties properties) {
        this.applicationRepository = applicationRepository;
        this.mailService = mailService;
        this.properties = properties;
    }

    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        applicationRepository.findById(event.applicationId()).ifPresent(application ->
                application.getApplicantAccount().ifPresent(account -> send(application, account,
                        ApplicantMails.received(account.getFirstName(), account.getLastName(),
                                application.getJobPosition().getTitle(), portalUrl()))));
    }

    /**
     * Interne Schritte (z. B. Eingegangen → KI-analysiert) sind für Bewerber:innen unsichtbar und lösen keine
     * Mail aus.
     */
    @EventListener
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        ApplicantStatus visible = ApplicantStatus.from(event.current());
        if (ApplicantStatus.from(event.previous()) == visible) {
            return;
        }
        applicationRepository.findById(event.applicationId()).ifPresent(application ->
                application.getApplicantAccount().ifPresent(account -> send(application, account,
                        ApplicantMails.statusChanged(visible, account.getFirstName(), account.getLastName(),
                                application.getJobPosition().getTitle(), portalUrl()))));
    }

    private void send(Application application, UserAccount account, ApplicantMails.Mail mail) {
        mailService.queue(account.getEmail(), mail.subject(), mail.body(), application.getId());
    }

    private String portalUrl() {
        String base = properties.mail().appBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + PORTAL_PATH;
    }
}
