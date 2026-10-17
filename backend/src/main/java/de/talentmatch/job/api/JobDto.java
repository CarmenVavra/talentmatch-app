package de.talentmatch.job.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import de.talentmatch.application.ApplicationStatus;

/**
 * Stelle inkl. Statuszählern und durchschnittlichem Match-Score (null, solange keine Analyse existiert).
 */
public record JobDto(
        Long id,
        String title,
        String department,
        String experienceLevel,
        String location,
        String description,
        List<String> requiredSkills,
        LocalDate createdDate,
        Map<ApplicationStatus, Long> statusCounts,
        long totalApplications,
        Integer averageScore) {
}
