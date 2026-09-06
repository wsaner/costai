package com.ruoyi.cost.knowledge.parse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfKnowledgeDocumentParser implements KnowledgeDocumentParser
{
    @Override
    public boolean supports(String extension) { return "pdf".equalsIgnoreCase(extension); }

    @Override
    public ParsedDocument parse(Path path) throws Exception
    {
        try (PDDocument document = Loader.loadPDF(path.toFile()))
        {
            if (!document.getCurrentAccessPermission().canExtractContent())
            {
                throw new IllegalArgumentException("PDF禁止提取文本");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            List<ParsedBlock> blocks = new ArrayList<>();
            int meaningfulChars = 0;
            for (int page = 1; page <= document.getNumberOfPages(); page++)
            {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                String heading = null;
                for (String paragraph : text.split("(?:\\R\\s*){2,}"))
                {
                    String normalized = TextParsingSupport.normalize(paragraph.replace('\n', ' '));
                    if (normalized.isEmpty()) continue;
                    if (TextParsingSupport.isHeading(normalized)) heading = normalized;
                    blocks.add(new ParsedBlock(page, heading, normalized));
                    meaningfulChars += normalized.replaceAll("\\s", "").length();
                }
            }
            if (meaningfulChars < Math.max(20, document.getNumberOfPages() * 5))
            {
                throw new OcrRequiredException("PDF未提取到足够文本，可能为扫描件，需要OCR");
            }
            return new ParsedDocument(blocks, document.getNumberOfPages());
        }
    }
}
