package de.talentmatch.mail.api;

import java.time.LocalDateTime;

import de.talentmatch.mail.MailStatus;

public record OutgoingMailDto(
        Long id,
        String recipient,
        String subject,
        String body,
        LocalDateTime createdAt,
        MailStatus status,
        String error,
        Long applicationId) {
}
