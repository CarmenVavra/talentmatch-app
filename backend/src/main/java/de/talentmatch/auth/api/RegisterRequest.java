package de.talentmatch.auth.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Selbstregistrierung für Bewerber:innen.
 */
public record RegisterRequest(
        @NotBlank(message = "Vorname ist erforderlich.")
        @Size(max = 100, message = "Der Vorname darf höchstens 100 Zeichen lang sein.")
        String firstName,
        @NotBlank(message = "Nachname ist erforderlich.")
        @Size(max = 100, message = "Der Nachname darf höchstens 100 Zeichen lang sein.")
        String lastName,
        @NotBlank(message = "E-Mail ist erforderlich.")
        @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
        String email,
        @NotBlank(message = "Passwort ist erforderlich.")
        @Size(min = 8, max = 100, message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
        String password,
        @NotNull(message = "Bitte bestätigen Sie, dass Sie die Datenschutzhinweise gelesen haben.")
        @AssertTrue(message = "Bitte bestätigen Sie, dass Sie die Datenschutzhinweise gelesen haben.")
        Boolean privacyAccepted) {
}
