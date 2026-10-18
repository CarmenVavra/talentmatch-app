package de.talentmatch.applicant.api;

import java.time.LocalDateTime;

import de.talentmatch.applicant.ApplicantStatus;
import de.talentmatch.talentpool.TalentPoolStatus;

/**
 * Eigene Bewerbung im Portal – bewusst nur Status, kein Match-Score und keine KI-Bewertung. Die
 * Evidenz-Felder (inkl. Link-Schlüssel für die Antwortseite) erhält nur die Bewerber:in selbst.
 */
public record MyApplicationDto(
        Long id,
        Long jobId,
        String jobTitle,
        String department,
        String location,
        ApplicantStatus status,
        LocalDateTime submittedAt,
        String resumeFileName,
        String skillProfileFileName,
        String coverLetterFileName,
        TalentPoolStatus talentPoolStatus,
        String talentPoolToken,
        LocalDateTime talentPoolDeadline,
        LocalDateTime talentPoolExpiresAt) {
}
