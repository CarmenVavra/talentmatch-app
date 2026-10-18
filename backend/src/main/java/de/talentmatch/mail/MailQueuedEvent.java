package de.talentmatch.mail;

/**
 * Eine E-Mail wurde gespeichert und soll nach dem Commit über den Mailserver versendet werden.
 */
public record MailQueuedEvent(Long mailId) {
}
