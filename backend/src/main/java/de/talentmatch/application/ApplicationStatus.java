package de.talentmatch.application;

/**
 * Phasen einer Bewerbung (Spalten der Kanban-Pipeline).
 */
public enum ApplicationStatus {
    NEW,
    MATCHED,
    INTERVIEW,
    HIRED,
    REJECTED
}
