package de.talentmatch.mail;

/**
 * Zustand einer E-Mail im Postausgang.
 */
public enum MailStatus {
    /** Wird gerade über den Mailserver versendet. */
    QUEUED,
    SENT,
    FAILED,
    /** Kein Mailserver konfiguriert – nur im Postausgang abgelegt. */
    NOT_SENT
}
