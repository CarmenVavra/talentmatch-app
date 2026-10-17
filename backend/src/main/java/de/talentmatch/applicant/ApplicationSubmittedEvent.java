package de.talentmatch.applicant;

/**
 * Wird veröffentlicht, wenn eine Bewerber:in über das Portal eine Bewerbung eingereicht hat.
 */
public record ApplicationSubmittedEvent(Long applicationId) {
}
