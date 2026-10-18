package de.talentmatch.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import de.talentmatch.ai.RecruitingAiService;
import de.talentmatch.common.exception.AiAnalysisException;
import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.resume.PdfDocumentReader.ReadDocument;

class PdfDocumentReaderTest {

    private static final byte[] PDF = "%PDF-1.7 Bild-PDF".getBytes(StandardCharsets.US_ASCII);
    private static final List<byte[]> PAGES = List.of(new byte[] {1}, new byte[] {2});

    private ResumeTextExtractor textExtractor;
    private PdfPageRenderer pageRenderer;
    private RecruitingAiService aiService;
    private PdfDocumentReader reader;

    @BeforeEach
    void setUp() throws IOException {
        textExtractor = mock(ResumeTextExtractor.class);
        pageRenderer = mock(PdfPageRenderer.class);
        aiService = mock(RecruitingAiService.class);
        TalentMatchProperties properties = new TalentMatchProperties(null, null,
                new TalentMatchProperties.Upload(10_485_760), null, null, null, null, null);
        reader = new PdfDocumentReader(textExtractor, pageRenderer, aiService, properties);
        when(textExtractor.extractText(any())).thenReturn("");
        when(pageRenderer.renderPages(any())).thenReturn(PAGES);
    }

    @Test
    void usesTheEmbeddedTextWithoutRecognition() {
        when(textExtractor.extractText(any())).thenReturn("Lena Hoffmann – Java");

        ReadDocument document = reader.read(DocumentType.RESUME, file("lebenslauf.pdf"));

        assertThat(document.text()).isEqualTo("Lena Hoffmann – Java");
        verify(aiService, never()).transcribePages(any());
    }

    @Test
    void recognizesTheTextOfImageOnlyPdfsOnceAndReusesIt() {
        when(aiService.isConfigured()).thenReturn(true);
        when(aiService.transcribePages(PAGES)).thenReturn("Carmen Gregor\nAngular, Spring Boot");

        ReadDocument first = reader.read(DocumentType.RESUME, file("lebenslauf.pdf"));
        ReadDocument second = reader.read(DocumentType.RESUME, file("lebenslauf.pdf"));

        assertThat(first.text()).isEqualTo("Carmen Gregor\nAngular, Spring Boot");
        assertThat(second.text()).isEqualTo(first.text());
        verify(aiService, times(1)).transcribePages(PAGES);
    }

    @Test
    void explainsHowToFixAnImagePdfWithoutApiKey() {
        when(aiService.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> reader.read(DocumentType.RESUME, file("lebenslauf.pdf")))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageStartingWith("In „lebenslauf.pdf“ wurde kein lesbarer Text gefunden.")
                .hasMessageContaining("Microsoft Print to PDF")
                .hasMessageContaining("Speichern unter → PDF");
        verify(aiService, never()).transcribePages(any());
    }

    @Test
    void namesTheAdditionalDocumentWhenRecognitionFails() {
        when(aiService.isConfigured()).thenReturn(true);
        when(aiService.transcribePages(PAGES)).thenThrow(new AiAnalysisException("Zeitüberschreitung"));

        assertThatThrownBy(() -> reader.read(DocumentType.SKILL_PROFILE, file("skills.pdf")))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageStartingWith("Skill-Profil: In „skills.pdf“ wurde kein lesbarer Text gefunden.");
    }

    @Test
    void reportsBlankRecognitionResultsAsUnreadable() {
        when(aiService.isConfigured()).thenReturn(true);
        when(aiService.transcribePages(PAGES)).thenReturn("  ");

        assertThatThrownBy(() -> reader.read(DocumentType.RESUME, file("leer.pdf")))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("kein lesbarer Text");
    }

    private static MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }
}
