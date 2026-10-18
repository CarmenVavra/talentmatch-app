package de.talentmatch.resume;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class PdfPageRendererTest {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    private final PdfPageRenderer renderer = new PdfPageRenderer();

    @Test
    void rendersEachPageAsPng() throws IOException {
        List<byte[]> pages = renderer.renderPages(pdfWithPages(2));

        assertThat(pages).hasSize(2)
                .allSatisfy(png -> assertThat(Arrays.copyOf(png, 4)).isEqualTo(PNG_MAGIC));
    }

    @Test
    void rendersAtMostTheFirstPages() throws IOException {
        assertThat(renderer.renderPages(pdfWithPages(PdfPageRenderer.MAX_PAGES + 2)))
                .hasSize(PdfPageRenderer.MAX_PAGES);
    }

    private static byte[] pdfWithPages(int count) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < count; i++) {
                document.addPage(new PDPage());
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
