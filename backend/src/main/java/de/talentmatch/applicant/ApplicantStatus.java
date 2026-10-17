package de.talentmatch.applicant;

import de.talentmatch.application.ApplicationStatus;

/**
 * Für Bewerber:innen sichtbarer Status. Interne Schritte (z. B. „KI-analysiert“) werden nicht offengelegt.
 */
public enum ApplicantStatus {
    RECEIVED,
    INTERVIEW,
    OFFER,
    REJECTED;

    public static ApplicantStatus from(ApplicationStatus status) {
        return switch (status) {
            case NEW, MATCHED -> RECEIVED;
            case INTERVIEW -> INTERVIEW;
            case HIRED -> OFFER;
            case REJECTED -> REJECTED;
        };
    }
}
