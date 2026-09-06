package com.ruoyi.cost.knowledge.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.knowledge.parse.ParsedBlock;

class KnowledgeChunkerTest
{
    private final KnowledgeChunker chunker = new KnowledgeChunker();

    @Test
    void shouldRespectPageAndSectionBoundaries()
    {
        String longParagraph = "工程造价审核依据。".repeat(70);
        List<KnowledgeChunkDraft> chunks = chunker.chunk(List.of(
                new ParsedBlock(1, "第一章 总则", longParagraph),
                new ParsedBlock(2, "第二章 计价", "材料价格应按合同约定调整。".repeat(45))));
        assertTrue(chunks.size() >= 2);
        assertEquals(1, chunks.get(0).pageNumber());
        assertEquals("第二章 计价", chunks.get(chunks.size() - 1).sectionTitle());
        assertTrue(chunks.stream().allMatch(item -> item.content().length() <= KnowledgeChunker.MAX_CHARS));
    }

    @Test
    void shouldKeepOverlapForLongText()
    {
        String text = "第一个审核句子。".repeat(220);
        List<KnowledgeChunkDraft> chunks = chunker.chunk(List.of(new ParsedBlock(3, "审核方法", text)));
        assertTrue(chunks.size() > 1);
        String previousTail = chunks.get(0).content().substring(Math.max(0, chunks.get(0).content().length() - 80));
        assertFalse(previousTail.isBlank());
        assertTrue(chunks.get(1).content().contains(previousTail.substring(previousTail.length() / 2)));
    }
}
