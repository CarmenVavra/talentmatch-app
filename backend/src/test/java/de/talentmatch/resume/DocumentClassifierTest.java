package de.talentmatch.resume;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentClassifierTest {

    @Test
    void fileNameDecidesFirst() {
        assertThat(DocumentClassifier.classify("Skillprofil_Carmen_Gregor.pdf", "Berufserfahrung …"))
                .isEqualTo(DocumentType.SKILL_PROFILE);
        assertThat(DocumentClassifier.classify("Anschreiben Gregor.pdf", "")).isEqualTo(DocumentType.COVER_LETTER);
        assertThat(DocumentClassifier.classify("Motivationsschreiben.pdf", "")).isEqualTo(DocumentType.COVER_LETTER);
        assertThat(DocumentClassifier.classify("CV_Gregor.pdf", "Sehr geehrte … Mit freundlichen Grüßen"))
                .isEqualTo(DocumentType.RESUME);
        assertThat(DocumentClassifier.classify("Kompetenzprofil.pdf", "")).isEqualTo(DocumentType.SKILL_PROFILE);
    }

    @Test
    void textDecidesWhenTheFileNameIsNeutral() {
        assertThat(DocumentClassifier.classify("scan_0815.pdf",
                "Sehr geehrte Frau Vogel,\nhiermit bewerbe ich mich …\nMit freundlichen Grüßen"))
                .isEqualTo(DocumentType.COVER_LETTER);
        assertThat(DocumentClassifier.classify("dokument.pdf",
                "Skills\nProgrammiersprachen: Java, TypeScript\nFrameworks: Angular, Spring\nTools: Git"))
                .isEqualTo(DocumentType.SKILL_PROFILE);
        assertThat(DocumentClassifier.classify("dokument.pdf",
                "Berufserfahrung\n2020–heute Entwicklerin\nAusbildung\nStudium Informatik\nKenntnisse: Java"))
                .isEqualTo(DocumentType.RESUME);
    }

    @Test
    void defaultsToResume() {
        assertThat(DocumentClassifier.classify(null, null)).isEqualTo(DocumentType.RESUME);
        assertThat(DocumentClassifier.classify("Carmen_Gregor.pdf", "Carmen Gregor, Wien"))
                .isEqualTo(DocumentType.RESUME);
    }
}
