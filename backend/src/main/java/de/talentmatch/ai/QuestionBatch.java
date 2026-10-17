package de.talentmatch.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Structured Output für „Weitere Fragen generieren“.
 */
public record QuestionBatch(
        @JsonPropertyDescription("Neue Interviewfragen, die sich nicht mit den bisherigen überschneiden")
        List<QuestionSuggestion> questions) {

    public QuestionBatch {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
