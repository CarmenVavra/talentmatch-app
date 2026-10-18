package de.talentmatch.talentpool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Von HR geprüfte bzw. angepasste Evidenz-Anfrage.
 */
public record TalentPoolRequest(
        @NotBlank(message = "Betreff ist erforderlich.")
        @Size(max = 300, message = "Der Betreff darf höchstens 300 Zeichen lang sein.")
        String subject,
        @NotBlank(message = "Der Text der E-Mail ist erforderlich.")
        @Size(max = 10_000, message = "Der Text darf höchstens 10.000 Zeichen lang sein.")
        String body) {
}
