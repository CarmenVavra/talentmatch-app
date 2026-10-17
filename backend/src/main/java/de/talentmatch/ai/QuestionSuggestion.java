package de.talentmatch.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Vom LLM vorgeschlagene Interviewfrage.
 */
public record QuestionSuggestion(
        @JsonPropertyDescription("Kurzes Thema, z. B. 'Architektur', 'Fehlender Skill', 'Werdegang', 'Technik', "
                + "'Soft Skills' oder 'Motivation'")
        String topic,
        @JsonPropertyDescription("Die Frage auf Deutsch in Sie-Form")
        String question) {
}
