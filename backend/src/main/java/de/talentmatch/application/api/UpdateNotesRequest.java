package de.talentmatch.application.api;

import jakarta.validation.constraints.Size;

public record UpdateNotesRequest(
        @Size(max = 10_000, message = "Notizen dürfen höchstens 10.000 Zeichen lang sein.")
        String notes) {
}
