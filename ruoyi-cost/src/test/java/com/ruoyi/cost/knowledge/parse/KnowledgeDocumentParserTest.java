package com.ruoyi.cost.knowledge.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KnowledgeDocumentParserTest
{
    @TempDir Path tempDir;

    @Test
    void shouldParseTextPdfByPage() throws Exception
    {
        Path path = tempDir.resolve("rule.pdf");
        try (PDDocument pdf = new PDDocument())
        {
            PDPage page = new PDPage(); pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page))
            {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700); content.showText("Cost review rule and pricing basis paragraph with enough searchable text."); content.endText();
            }
            pdf.save(path.toFile());
        }
        ParsedDocument result = new PdfKnowledgeDocumentParser().parse(path);
        assertEquals(1, result.pageCount());
        assertEquals(1, result.blocks().get(0).pageNumber());
        assertTrue(result.blocks().get(0).text().contains("pricing basis"));
    }

    @Test
    void shouldMarkImageOnlyPdfAsOcrRequired() throws Exception
    {
        Path path = tempDir.resolve("scan.pdf");
        try (PDDocument pdf = new PDDocument()) { pdf.addPage(new PDPage()); pdf.save(path.toFile()); }
        assertThrows(OcrRequiredException.class, () -> new PdfKnowledgeDocumentParser().parse(path));
    }

    @Test
    void shouldPreserveDocxHeadingAndTable() throws Exception
    {
        Path path = tempDir.resolve("basis.docx");
        try (XWPFDocument doc = new XWPFDocument(); OutputStream output = Files.newOutputStream(path))
        {
            var heading = doc.createParagraph(); heading.setStyle("Heading1"); heading.createRun().setText("第一章 总则");
            doc.createParagraph().createRun().setText("本规则适用于工程造价审核。".repeat(4));
            var table = doc.createTable(1, 2); table.getRow(0).getCell(0).setText("项目编码"); table.getRow(0).getCell(1).setText("综合单价");
            doc.write(output);
        }
        ParsedDocument result = new DocxKnowledgeDocumentParser().parse(path);
        assertTrue(result.blocks().stream().anyMatch(item -> "第一章 总则".equals(item.sectionTitle())));
        assertTrue(result.blocks().stream().anyMatch(item -> item.text().contains("项目编码")));
    }

    @Test
    void shouldFallbackToGb18030ForTxt() throws Exception
    {
        Path path = tempDir.resolve("basis.txt");
        Files.write(path, "第一章 总则\r\n\r\n安徽省建设工程计价依据。".getBytes(Charset.forName("GB18030")));
        ParsedDocument result = new TxtKnowledgeDocumentParser().parse(path);
        assertEquals("第一章 总则", result.blocks().get(1).sectionTitle());
    }
}
