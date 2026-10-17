package de.talentmatch.candidate;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Strukturiert extrahierter Lebenslauf (Structured Output des LLM, gespeichert als JSON).
 */
public record CandidateProfile(
        @JsonPropertyDescription("Vorname der Person")
        String firstName,
        @JsonPropertyDescription("Nachname der Person")
        String lastName,
        @JsonPropertyDescription("E-Mail-Adresse oder null")
        String email,
        @JsonPropertyDescription("Telefonnummer oder null")
        String phone,
        @JsonPropertyDescription("Wohnort (Stadt) oder null")
        String city,
        @JsonPropertyDescription("Aktuelle bzw. letzte Berufsbezeichnung")
        String currentRole,
        @JsonPropertyDescription("Aktueller bzw. letzter Arbeitgeber")
        String currentCompany,
        @JsonPropertyDescription("Berufserfahrung, neueste zuerst")
        List<WorkExperience> positions,
        @JsonPropertyDescription("Technologien, Tools und Fachkenntnisse als kurze Begriffe")
        List<String> technologies,
        @JsonPropertyDescription("Höchster Abschluss, z. B. 'M.Sc. Informatik · TU München · 2018'")
        String education,
        @JsonPropertyDescription("Sprachen mit Niveau, z. B. 'Englisch (C1)'")
        List<String> languages,
        @JsonPropertyDescription("Gesamte Berufserfahrung in Jahren (gerundet)")
        Integer yearsOfExperience) {

    public CandidateProfile {
        positions = positions == null ? List.of() : List.copyOf(positions);
        technologies = technologies == null ? List.of() : List.copyOf(technologies);
        languages = languages == null ? List.of() : List.copyOf(languages);
    }

    /**
     * Eine Station im Werdegang.
     */
    public record WorkExperience(
            @JsonPropertyDescription("Zeitraum im Format 'MM/JJJJ – MM/JJJJ' oder 'MM/JJJJ – heute'")
            String period,
            @JsonPropertyDescription("Berufsbezeichnung")
            String role,
            @JsonPropertyDescription("Arbeitgeber")
            String company,
            @JsonPropertyDescription("Ort")
            String location,
            @JsonPropertyDescription("2–3 kurze Stichpunkte zu Aufgaben und Erfolgen")
            List<String> highlights) {

        public WorkExperience {
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
        }
    }
}
