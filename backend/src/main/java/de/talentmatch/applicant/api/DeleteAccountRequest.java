package de.talentmatch.applicant.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Bestätigung der Kontolöschung mit dem aktuellen Passwort.
 */
public record DeleteAccountRequest(@NotBlank(message = "Passwort ist erforderlich.") String password) {
}
