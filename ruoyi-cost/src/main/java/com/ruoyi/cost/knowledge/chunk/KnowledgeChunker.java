package com.ruoyi.cost.knowledge.chunk;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.knowledge.parse.ParsedBlock;

/** 按页、标题和段落组织语义分片，并在相邻分片间保留句尾重叠。 */
@Component
public class KnowledgeChunker
{
    static final int TARGET_CHARS = 1000;
    static final int MAX_CHARS = 1400;
    static final int OVERLAP_CHARS = 150;

    public List<KnowledgeChunkDraft> chunk(List<ParsedBlock> blocks)
    {
        List<KnowledgeChunkDraft> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Integer page = null;
        String title = null;
        for (ParsedBlock block : blocks)
        {
            for (String part : splitLong(block.text()))
            {
                boolean boundary = buffer.length() > 0 && (!equals(page, block.pageNumber())
                        || !equals(title, block.sectionTitle())) && buffer.length() >= TARGET_CHARS / 2;
                if (boundary)
                {
                    add(result, page, title, buffer.toString());
                    buffer.setLength(0);
                    page = block.pageNumber(); title = block.sectionTitle();
                }
                else if (buffer.length() + part.length() + 1 > MAX_CHARS)
                {
                    add(result, page, title, buffer.toString());
                    String overlap = tail(buffer.toString());
                    buffer.setLength(0);
                    if (!overlap.isEmpty()) buffer.append(overlap).append('\n');
                }
                if (buffer.length() == 0) { page = block.pageNumber(); title = block.sectionTitle(); }
                buffer.append(part).append('\n');
            }
        }
        add(result, page, title, buffer.toString());
        return result;
    }

    private List<String> splitLong(String text)
    {
        List<String> parts = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (remaining.length() > MAX_CHARS)
        {
            int cut = Math.min(TARGET_CHARS, remaining.length());
            int sentence = Math.max(remaining.lastIndexOf('。', cut), Math.max(remaining.lastIndexOf('；', cut), remaining.lastIndexOf('\n', cut)));
            if (sentence >= TARGET_CHARS / 2) cut = sentence + 1;
            parts.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) parts.add(remaining);
        return parts;
    }

    private void add(List<KnowledgeChunkDraft> result, Integer page, String title, String content)
    {
        String value = content == null ? "" : content.trim();
        if (!value.isEmpty()) result.add(new KnowledgeChunkDraft(page, title, result.size(), value));
    }

    private String tail(String value)
    {
        if (value.length() <= OVERLAP_CHARS) return value.trim();
        int start = value.length() - OVERLAP_CHARS;
        int boundary = Math.max(value.indexOf('。', start), value.indexOf('\n', start));
        return value.substring(boundary >= 0 ? boundary + 1 : start).trim();
    }

    private boolean equals(Object left, Object right) { return left == null ? right == null : left.equals(right); }
}
