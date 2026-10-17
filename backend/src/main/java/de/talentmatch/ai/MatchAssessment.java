package de.talentmatch.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Structured Output des LLM für Workflow 2 (Match Analysis &amp; Scoring).
 * Der Skill-Score wird regelbasiert berechnet; das LLM bewertet Erfahrung, Ausbildung und Sprachen.
 */
public record MatchAssessment(
        @JsonPropertyDescription("Passung der Berufserfahrung (Dauer, Seniorität, Branche) zur Stelle, 0-100")
        int experienceScore,
        @JsonPropertyDescription("Passung von Ausbildung/Abschluss zur Stelle, 0-100")
        int educationScore,
        @JsonPropertyDescription("Passung der Sprachkenntnisse (Deutsch/Englisch im Arbeitskontext), 0-100")
        int languagesScore,
        @JsonPropertyDescription("Management Summary auf Deutsch, 3-4 Sätze, sachlich, mit Empfehlung")
        String summary,
        @JsonPropertyDescription("Risikofaktoren auf Deutsch, z. B. Lücken im Lebenslauf oder kurze Verweildauer")
        List<String> riskFactors,
        @JsonPropertyDescription("4-5 maßgeschneiderte Interviewfragen zu Wissenslücken und Unklarheiten")
        List<QuestionSuggestion> interviewQuestions) {

    public MatchAssessment {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        interviewQuestions = interviewQuestions == null ? List.of() : List.copyOf(interviewQuestions);
    }
}
