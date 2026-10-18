package de.talentmatch.resume;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import de.talentmatch.common.exception.InvalidFileException;
import de.talentmatch.config.TalentMatchProperties;

/**
 * Prüft hochgeladene Bewerbungsunterlagen (PDF, Größe, Magic Bytes) und extrahiert ihren Text mit Tika.
 */
@Component
public class PdfDocumentReader {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final ResumeTextExtractor textExtractor;
    private final TalentMatchProperties properties;

    public PdfDocumentReader(ResumeTextExtractor textExtractor, TalentMatchProperties properties) {
        this.textExtractor = textExtractor;
        this.properties = properties;
    }

    /**
     * Liest ein PDF ein. Fehlermeldungen zu Zusatzunterlagen nennen das betroffene Dokument.
     *
     * @throws InvalidFileException bei leerer, zu großer, falscher oder textloser Datei
     */
    public ReadDocument read(DocumentType type, MultipartFile file) {
        byte[] data = validate(type, file);
        String text = textExtractor.extractText(data);
        if (text.isBlank()) {
            throw new InvalidFileException(prefix(type) + "Im PDF wurde kein Text gefunden (gescanntes Dokument?).");
        }
        return new ReadDocument(fileName(type, file), data, text);
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
