package de.talentmatch.application.api;

public record CandidateSummaryDto(
        Long id,
        String firstName,
        String lastName,
        String currentRole,
        String currentCompany,
        String city) {
}
