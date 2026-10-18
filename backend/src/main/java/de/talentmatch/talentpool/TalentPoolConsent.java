package de.talentmatch.talentpool;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Einwilligung zur Evidenz-Haltung einer Bewerbung – als Nachweis mit allen Zeitpunkten. Liegt direkt an der
 * Bewerbung (keine Zusatzabfragen in Listen); sind alle Spalten leer, ist die Bewerbung nicht in Evidenz.
 */
@Embeddable
public class TalentPoolConsent {

    @Enumerated(EnumType.STRING)
    @Column(name = "talent_pool_status", length = 16)
    private TalentPoolStatus status;

    /** Geheimer Schlüssel für die Antwortseite (ohne Anmeldung). */
    @Column(name = "talent_pool_token", length = 64, unique = true)
    private String token;

    @Column(name = "talent_pool_requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "talent_pool_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "talent_pool_reminded_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "talent_pool_confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "talent_pool_expires_at")
    private LocalDateTime expiresAt;

    protected TalentPoolConsent() {
    }

    public static TalentPoolConsent request(String token, LocalDateTime now, LocalDateTime responseDeadline) {
        TalentPoolConsent consent = new TalentPoolConsent();
        consent.status = TalentPoolStatus.REQUESTED;
        consent.token = token;
        consent.requestedAt = now;
        consent.responseDeadline = responseDeadline;
        return consent;
    }

    /**
     * Zustimmung der Bewerber:in – die Evidenz gilt bis {@code expiresAt}.
     */
    public void confirm(LocalDateTime now, LocalDateTime expiresAt) {
        this.status = TalentPoolStatus.CONFIRMED;
        this.confirmedAt = now;
        this.expiresAt = expiresAt;
    }

    public void markReminded(LocalDateTime now) {
        this.reminderSentAt = now;
    }

    public TalentPoolStatus getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getResponseDeadline() {
        return responseDeadline;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
