package de.talentmatch.common.exception;

/**
 * Eine fachliche Regel verhindert die Aktion (z. B. Interviewfragen ohne vorhandene Analyse) – HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
