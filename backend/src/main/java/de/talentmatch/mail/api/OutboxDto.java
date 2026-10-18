package de.talentmatch.mail.api;

import java.util.List;

/**
 * Postausgang: {@code deliveryEnabled} ist false, solange kein Mailserver (SMTP) konfiguriert ist.
 */
public record OutboxDto(boolean deliveryEnabled, List<OutgoingMailDto> mails) {
}
