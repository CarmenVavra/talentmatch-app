package de.talentmatch.talentpool.api;

import java.time.LocalDateTime;

import de.talentmatch.talentpool.TalentPoolStatus;

/**
 * Evidenz-Stand einer Bewerbung für HR.
 */
public record TalentPoolInfoDto(
        TalentPoolStatus status,
        LocalDateTime requestedAt,
        LocalDateTime responseDeadline,
        LocalDateTime reminderSentAt,
        LocalDateTime confirmedAt,
        LocalDateTime expiresAt) {
}
