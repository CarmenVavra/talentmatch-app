package de.talentmatch.resume;

/**
 * Original-PDF zur Auslieferung an die PDF-Vorschau.
 */
public record ResumeFile(String fileName, String contentType, byte[] data) {

    public ResumeFile {
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }
}
