package de.talentmatch.auth;

/**
 * Rollen der Anwendung. Werden als Claim {@code roles} im JWT ausgeliefert.
 */
public enum Role {
    HR_MANAGER,
    APPLICANT
}
