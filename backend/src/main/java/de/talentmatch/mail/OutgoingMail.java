package de.talentmatch.mail;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * E-Mail im Postausgang. {@code applicationId} verknüpft sie mit einer Bewerbung, damit sie beim Löschen der
 * Bewerbung mitgelöscht wird (keine personenbezogenen Daten ohne Bewerbung).
 */
@Entity
@Table(name = "outgoing_mail")
public class OutgoingMail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, length = 20_000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MailStatus status;

    @Column(length = 1000)
    private String error;

    private Long applicationId;

    protected OutgoingMail() {
    }

    public OutgoingMail(String recipient, String subject, String body, LocalDateTime createdAt, MailStatus status,
                        Long applicationId) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
        this.status = status;
        this.applicationId = applicationId;
    }

    public void markSent() {
        this.status = MailStatus.SENT;
        this.error = null;
    }

    public void markFailed(String message) {
        this.status = MailStatus.FAILED;
        this.error = message == null ? "Unbekannter Fehler" : message.substring(0, Math.min(1000, message.length()));
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public MailStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Long getApplicationId() {
        return applicationId;
    }
}
