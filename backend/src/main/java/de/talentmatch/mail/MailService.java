package de.talentmatch.mail;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.mail.api.OutboxDto;
import de.talentmatch.mail.api.OutgoingMailDto;

/**
 * Legt E-Mails im Postausgang ab. Ist ein Mailserver konfiguriert ({@code spring.mail.host}), werden sie nach dem
 * Commit im Hintergrund versendet; sonst bleiben sie nur im Postausgang (lokal gefahrlos testbar).
 */
@Service
public class MailService {

    private final OutgoingMailRepository repository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public MailService(OutgoingMailRepository repository, ObjectProvider<JavaMailSender> mailSender,
                       ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.events = events;
        this.clock = clock;
    }

    public boolean deliveryEnabled() {
        return mailSender.getIfAvailable() != null;
    }

    /**
     * Muss innerhalb einer Transaktion aufgerufen werden – der Versand startet erst nach dem Commit.
     */
    @Transactional
    public OutgoingMail queue(String recipient, String subject, String body, Long applicationId) {
        boolean deliver = deliveryEnabled();
        OutgoingMail mail = repository.save(new OutgoingMail(recipient, subject, body, LocalDateTime.now(clock),
                deliver ? MailStatus.QUEUED : MailStatus.NOT_SENT, applicationId));
        if (deliver) {
            events.publishEvent(new MailQueuedEvent(mail.getId()));
        }
        return mail;
    }

    @Transactional(readOnly = true)
    public OutboxDto outbox() {
        return new OutboxDto(deliveryEnabled(),
                repository.findAllByOrderByCreatedAtDescIdDesc().stream().map(MailService::toDto).toList());
    }

    @Transactional(readOnly = true)
    public List<OutgoingMailDto> mailsFor(Long applicationId) {
        return repository.findByApplicationIdOrderByCreatedAtDescIdDesc(applicationId).stream()
                .map(MailService::toDto).toList();
    }

    static OutgoingMailDto toDto(OutgoingMail mail) {
        return new OutgoingMailDto(mail.getId(), mail.getRecipient(), mail.getSubject(), mail.getBody(),
                mail.getCreatedAt(), mail.getStatus(), mail.getError(), mail.getApplicationId());
    }
}
