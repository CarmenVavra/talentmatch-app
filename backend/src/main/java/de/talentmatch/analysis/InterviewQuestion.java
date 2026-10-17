package de.talentmatch.analysis;

/**
 * Generierte Interviewfrage mit Themen-Kicker und Besprochen-Status (Interview-Helper).
 */
public record InterviewQuestion(String topic, String text, boolean discussed) {

    public InterviewQuestion withDiscussed(boolean value) {
        return new InterviewQuestion(topic, text, value);
    }
}
