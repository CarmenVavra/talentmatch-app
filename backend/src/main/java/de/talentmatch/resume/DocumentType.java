package de.talentmatch.resume;

/**
 * Bewerbungsunterlagen: Der Lebenslauf ist Pflicht, Skill-Profil und Anschreiben sind optional.
 */
public enum DocumentType {

    RESUME("Lebenslauf"),
    SKILL_PROFILE("Skill-Profil"),
    COVER_LETTER("Anschreiben");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Ersatz-Dateiname, falls der Browser keinen mitsendet.
     */
    public String defaultFileName() {
        return label + ".pdf";
    }
}
