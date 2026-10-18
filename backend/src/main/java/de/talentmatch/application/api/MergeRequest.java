package de.talentmatch.application.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import de.talentmatch.resume.DocumentType;

/**
 * Führt die Bewerbung {@code sourceId} in die aufgerufene Bewerbung über. Nur die in {@code documents}
 * genannten Unterlagen werden übernommen – jeweils als die angegebene Dokumentart.
 */
public record MergeRequest(
        @NotNull(message = "Die zu übernehmende Bewerbung fehlt.")
        Long sourceId,
        @NotNull(message = "Bitte angeben, welche Unterlagen übernommen werden.")
        List<@Valid @NotNull DocumentMove> documents) {

    /**
     * Unterlage {@code from} der Quelle wird als {@code to} in der Ziel-Bewerbung abgelegt.
     */
    public record DocumentMove(
            @NotNull(message = "Dokumentart fehlt.") DocumentType from,
            @NotNull(message = "Zielart fehlt.") DocumentType to) {
    }
}
