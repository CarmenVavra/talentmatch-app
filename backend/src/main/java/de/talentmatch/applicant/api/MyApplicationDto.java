package de.talentmatch.applicant.api;

import java.time.LocalDateTime;

import de.talentmatch.applicant.ApplicantStatus;

/**
 * Eigene Bewerbung im Portal – bewusst nur Status, kein Match-Score und keine KI-Bewertung.
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
        String coverLetterFileName) {
}
