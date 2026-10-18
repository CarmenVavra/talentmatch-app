package de.talentmatch.application;

/**
 * Wird innerhalb der Transaktion veröffentlicht, wenn HR den Status einer Bewerbung geändert hat.
 */
public record ApplicationStatusChangedEvent(Long applicationId, ApplicationStatus previous,
                                            ApplicationStatus current) {
}
