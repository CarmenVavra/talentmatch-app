package de.talentmatch.talentpool.api;

/**
 * Eintrag der Talentpool-Ansicht (alle Stellen).
 */
public record TalentPoolEntryDto(
        Long applicationId,
        String firstName,
        String lastName,
        String email,
        Long jobId,
        String jobTitle,
        TalentPoolInfoDto talentPool) {
}
