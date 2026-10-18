package de.talentmatch.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import de.talentmatch.config.TalentMatchProperties;

/**
 * Versendet gespeicherte E-Mails über den Mailserver – nach dem Commit und im Hintergrund, damit ein langsamer
 * oder nicht erreichbarer Mailserver weder Anfragen blockiert noch Daten zurückrollt.
 */
@Component
public class MailDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MailDispatcher.class);

    private final OutgoingMailRepository repository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final TransactionTemplate transactionTemplate;
    private final TalentMatchProperties properties;

    public MailDispatcher(OutgoingMailRepository repository, ObjectProvider<JavaMailSender> mailSender,
                          TransactionTemplate transactionTemplate, TalentMatchProperties properties) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener
    public void onMailQueued(MailQueuedEvent event) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> repository.findById(event.mailId()).ifPresent(mail -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(properties.mail().from());
                message.setTo(mail.getRecipient());
                message.setSubject(mail.getSubject());
                message.setText(mail.getBody());
                sender.send(message);
                mail.markSent();
            } catch (MailException ex) {
                LOG.warn("E-Mail {} konnte nicht versendet werden: {}", mail.getId(), ex.getMessage());
                mail.markFailed(ex.getMessage());
            }
        }));
    }
}
