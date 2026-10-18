package de.talentmatch.resume;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * Rendert die ersten Seiten eines PDFs als PNG – Grundlage der Texterkennung für Bild-PDFs.
 */
@Component
public class PdfPageRenderer {

    /** Bewerbungsunterlagen sind selten länger; begrenzt Kosten und Laufzeit der Texterkennung. */
    static final int MAX_PAGES = 5;
    /** Auflösung, bei der normale Schriftgrößen sicher lesbar bleiben. */
    private static final float DPI = 150;

    /**
     * @throws IOException wenn das PDF nicht gerendert werden kann
     */
    public List<byte[]> renderPages(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), MAX_PAGES);
            List<byte[]> images = new ArrayList<>(pages);
            for (int page = 0; page < pages; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, DPI, ImageType.RGB);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                images.add(out.toByteArray());
            }
            return images;
        }
    }
}
