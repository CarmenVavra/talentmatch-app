package de.talentmatch.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.talentmatch.config.TalentMatchProperties;

class MailServiceTest {

    private OutgoingMailRepository repository;
    private ApplicationEventPublisher events;
    private JavaMailSender sender;

    @BeforeEach
    void setUp() {
        repository = mock(OutgoingMailRepository.class);
        when(repository.save(any(OutgoingMail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        events = mock(ApplicationEventPublisher.class);
        sender = mock(JavaMailSender.class);
    }

    @Test
    void withoutMailServerTheMailOnlyLandsInTheOutbox() {
        MailService service = service(null);

        OutgoingMail mail = service.queue("max@example.org", "Betreff", "Text", 7L);

        assertThat(mail.getStatus()).isEqualTo(MailStatus.NOT_SENT);
        assertThat(service.deliveryEnabled()).isFalse();
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void withMailServerTheMailIsQueuedForDelivery() {
        MailService service = service(sender);

        OutgoingMail mail = service.queue("max@example.org", "Betreff", "Text", 7L);

        assertThat(mail.getStatus()).isEqualTo(MailStatus.QUEUED);
        verify(events).publishEvent(any(MailQueuedEvent.class));
    }

    @Test
    void dispatcherSendsAndMarksTheMail() {
        OutgoingMail mail = new OutgoingMail("max@example.org", "Betreff", "Text", LocalDateTime.now(),
                MailStatus.QUEUED, 7L);
        when(repository.findById(1L)).thenReturn(Optional.of(mail));

        dispatcher(sender).onMailQueued(new MailQueuedEvent(1L));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@talentmatch.local");
        assertThat(message.getValue().getTo()).containsExactly("max@example.org");
        assertThat(mail.getStatus()).isEqualTo(MailStatus.SENT);
    }

    @Test
    void dispatcherRecordsDeliveryErrors() {
        OutgoingMail mail = new OutgoingMail("max@example.org", "Betreff", "Text", LocalDateTime.now(),
                MailStatus.QUEUED, 7L);
        when(repository.findById(1L)).thenReturn(Optional.of(mail));
        doThrow(new MailSendException("Verbindung abgelehnt")).when(sender).send(any(SimpleMailMessage.class));

        dispatcher(sender).onMailQueued(new MailQueuedEvent(1L));

        assertThat(mail.getStatus()).isEqualTo(MailStatus.FAILED);
        assertThat(mail.getError()).contains("Verbindung abgelehnt");
    }

    private MailService service(JavaMailSender mailSender) {
        return new MailService(repository, provider(mailSender), events,
                Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC));
    }

    private MailDispatcher dispatcher(JavaMailSender mailSender) {
        TalentMatchProperties properties = new TalentMatchProperties(null, null, null, null,
                new TalentMatchProperties.Mail("noreply@talentmatch.local", "http://localhost:4200"),
                new TalentMatchProperties.TalentPool(Duration.ofDays(14), Duration.ofDays(7), Duration.ofDays(730)),
                new TalentMatchProperties.Privacy(Duration.ofDays(180)));
        return new MailDispatcher(repository, provider(mailSender),
                new TransactionTemplate(mock(PlatformTransactionManager.class)), properties);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }
}
