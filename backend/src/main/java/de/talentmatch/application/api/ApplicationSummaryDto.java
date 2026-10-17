package de.talentmatch.application.api;

import java.time.LocalDateTime;
import java.util.List;

import de.talentmatch.application.ApplicationStatus;

/**
 * Bewerbung für Kanban-Karten und die Bewerberliste. {@code overallScore} ist null, solange nicht analysiert.
 * {@code skills} enthält zuerst die zur Stelle passenden Skills, danach weitere Technologien.
 */
public record ApplicationSummaryDto(
        Long id,
        Long jobPositionId,
        String jobTitle,
        ApplicationStatus status,
        LocalDateTime createdAt,
        CandidateSummaryDto candidate,
        Integer overallScore,
        List<String> skills) {
}
