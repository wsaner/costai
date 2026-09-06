package com.ruoyi.cost.knowledge.parse;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;

@Component
public class DocxKnowledgeDocumentParser implements KnowledgeDocumentParser
{
    @Override
    public boolean supports(String extension) { return "docx".equalsIgnoreCase(extension); }

    @Override
    public ParsedDocument parse(Path path) throws Exception
    {
        List<ParsedBlock> blocks = new ArrayList<>();
        String heading = null;
        try (InputStream input = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(input))
        {
            for (XWPFParagraph paragraph : document.getParagraphs())
            {
                String text = TextParsingSupport.normalize(paragraph.getText());
                if (text.isEmpty()) continue;
                String style = paragraph.getStyle();
                if ((style != null && (style.toLowerCase().contains("heading") || style.contains("标题")))
                        || TextParsingSupport.isHeading(text)) heading = text;
                blocks.add(new ParsedBlock(null, heading, text));
            }
            for (XWPFTable table : document.getTables())
            {
                table.getRows().forEach(row -> {
                    String text = row.getTableCells().stream().map(cell -> TextParsingSupport.normalize(cell.getText()))
                            .filter(item -> !item.isEmpty()).reduce((a, b) -> a + " | " + b).orElse("");
                    if (!text.isEmpty()) blocks.add(new ParsedBlock(null, null, text));
                });
            }
        }
        if (blocks.isEmpty()) throw new IllegalArgumentException("DOCX没有可解析文本");
        return new ParsedDocument(blocks, 0);
    }
}
