package de.talentmatch.resume;

import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import de.talentmatch.common.exception.InvalidFileException;

/**
 * Extrahiert den Text eines PDF-Lebenslaufs mit Apache Tika (über den Spring-AI-DocumentReader).
 */
@Component
public class ResumeTextExtractor {

    public String extractText(byte[] pdf) {
        try {
            return new TikaDocumentReader(new ByteArrayResource(pdf)).get().stream()
                    .map(Document::getText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"))
                    .strip();
        } catch (RuntimeException ex) {
            throw new InvalidFileException("Das PDF konnte nicht gelesen werden.", ex);
        }
    }
}
