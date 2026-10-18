package de.talentmatch.resume;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.ai.RecruitingAiService;
import de.talentmatch.common.exception.AiAnalysisException;
import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.config.TalentMatchProperties;

/**
 * Prüft hochgeladene Bewerbungsunterlagen (PDF, Größe, Magic Bytes) und extrahiert ihren Text mit Tika.
 * Enthält ein PDF nur Bilder (eingescannt oder über „Microsoft Print to PDF“ gedruckt), liest die KI den Text
 * aus den gerenderten Seiten – sofern ein API-Key konfiguriert ist.
 */
@Component
public class PdfDocumentReader {

    private static final Logger LOG = LoggerFactory.getLogger(PdfDocumentReader.class);
    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);
    /** Erkannte Texte je PDF-Inhalt – HR-Vorschau und Upload lesen dieselbe Datei nacheinander. */
    private static final int MAX_CACHED = 32;

    private final ResumeTextExtractor textExtractor;
    private final PdfPageRenderer pageRenderer;
    private final RecruitingAiService aiService;
    private final TalentMatchProperties properties;
    private final Map<String, String> recognized = new ConcurrentHashMap<>();

    public PdfDocumentReader(ResumeTextExtractor textExtractor, PdfPageRenderer pageRenderer,
                             RecruitingAiService aiService, TalentMatchProperties properties) {
        this.textExtractor = textExtractor;
        this.pageRenderer = pageRenderer;
        this.aiService = aiService;
        this.properties = properties;
    }

    /**
     * Liest ein PDF ein. Fehlermeldungen zu Zusatzunterlagen nennen das betroffene Dokument.
     *
     * @throws InvalidFileException bei leerer, zu großer, falscher oder textloser Datei
     */
    public ReadDocument read(DocumentType type, MultipartFile file) {
        byte[] data = validate(type, file);
        String fileName = fileName(type, file);
        String text = textExtractor.extractText(data);
        if (text.isBlank()) {
            text = recognizeText(data, fileName)
                    .orElseThrow(() -> new InvalidFileException(noTextMessage(type, fileName)));
        }
        return new ReadDocument(fileName, data, text);
    }

    static String noTextMessage(DocumentType type, String fileName) {
        return prefix(type) + "In „" + fileName + "“ wurde kein lesbarer Text gefunden. Das passiert bei "
                + "eingescannten Dokumenten oder wenn das PDF über „Drucken → Microsoft Print to PDF“ erstellt wurde. "
                + "Bitte speichern Sie das Dokument direkt als PDF (z. B. in Word: Datei → Speichern unter → PDF) "
                + "und laden Sie es erneut hoch.";
    }

    /**
     * Texterkennung per KI für Bild-PDFs. Leer, wenn kein API-Key konfiguriert ist, die Erkennung fehlschlägt
     * oder die Seiten keinen Text enthalten.
     */
    private Optional<String> recognizeText(byte[] data, String fileName) {
        if (!aiService.isConfigured()) {
            return Optional.empty();
        }
        String key = hash(data);
        String cached = recognized.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            List<byte[]> pages = pageRenderer.renderPages(data);
            String text = pages.isEmpty() ? "" : aiService.transcribePages(pages);
            if (text.isBlank()) {
                return Optional.empty();
            }
            if (recognized.size() >= MAX_CACHED) {
                recognized.clear();
            }
            recognized.put(key, text);
            LOG.info("Texterkennung für Bild-PDF „{}“: {} Seite(n), {} Zeichen erkannt", fileName, pages.size(),
                    text.length());
            return Optional.of(text);
        } catch (IOException | AiAnalysisException ex) {
            LOG.warn("Texterkennung für „{}“ fehlgeschlagen: {}", fileName, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String hash(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nicht verfügbar", ex);
        }
    }

    static String fileName(DocumentType type, MultipartFile file) {
        return file == null || file.getOriginalFilename() == null
                ? type.defaultFileName() : file.getOriginalFilename();
    }

    private static String prefix(DocumentType type) {
        return type == DocumentType.RESUME ? "" : type.label() + ": ";
    }

    private byte[] validate(DocumentType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException(prefix(type) + "Die Datei ist leer.");
        }
        if (file.getSize() > properties.upload().maxFileSizeBytes()) {
            throw new InvalidFileException(prefix(type) + "Die Datei ist größer als 10 MB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidFileException(prefix(type) + "Die Datei konnte nicht gelesen werden.", ex);
        }
        boolean pdfMagic = data.length >= PDF_MAGIC.length
                && Arrays.equals(Arrays.copyOf(data, PDF_MAGIC.length), PDF_MAGIC);
        if (!name.endsWith(".pdf") || !pdfMagic) {
            throw new InvalidFileException(prefix(type) + "Falscher Dateityp – nur PDF-Dateien sind erlaubt.");
        }
        return data;
    }

    /**
     * Eingelesenes PDF: Dateiname, Rohdaten und extrahierter Text.
     */
    public record ReadDocument(String fileName, byte[] data, String text) {

        public ReadDocument {
            data = data.clone();
        }

        @Override
        public byte[] data() {
            return data.clone();
        }
    }
}
