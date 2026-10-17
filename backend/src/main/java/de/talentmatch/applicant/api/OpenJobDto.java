package de.talentmatch.applicant.api;

import java.time.LocalDate;
import java.util.List;

/**
 * Stelle aus Sicht der Bewerber:innen – ohne Bewerbungszahlen und Scores.
 */
public record OpenJobDto(
        Long id,
        String title,
        String department,
        String experienceLevel,
        String location,
        String description,
        List<String> requiredSkills,
        LocalDate createdDate,
        boolean applied) {
}
