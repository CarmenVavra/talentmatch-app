package de.talentmatch.common.exception;

/**
 * Die KI-Analyse (LLM-Aufruf) ist fehlgeschlagen oder nicht verfügbar – HTTP 502.
 */
public class AiAnalysisException extends RuntimeException {

    public AiAnalysisException(String message) {
        super(message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
