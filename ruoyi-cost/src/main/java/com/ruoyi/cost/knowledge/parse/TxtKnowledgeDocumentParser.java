package com.ruoyi.cost.knowledge.parse;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TxtKnowledgeDocumentParser implements KnowledgeDocumentParser
{
    @Override
    public boolean supports(String extension) { return "txt".equalsIgnoreCase(extension); }

    @Override
    public ParsedDocument parse(Path path) throws Exception
    {
        byte[] bytes = Files.readAllBytes(path);
        String text;
        try { text = decode(bytes, StandardCharsets.UTF_8); }
        catch (CharacterCodingException e) { text = decode(bytes, Charset.forName("GB18030")); }
        List<ParsedBlock> blocks = new ArrayList<>();
        String heading = null;
        for (String paragraph : text.split("(?:\\R\\s*){2,}"))
        {
            String normalized = TextParsingSupport.normalize(paragraph.replace('\n', ' '));
            if (normalized.isEmpty()) continue;
            if (TextParsingSupport.isHeading(normalized)) heading = normalized;
            blocks.add(new ParsedBlock(null, heading, normalized));
        }
        if (blocks.isEmpty()) throw new IllegalArgumentException("TXT没有可解析文本");
        return new ParsedDocument(blocks, 0);
    }

    private String decode(byte[] bytes, Charset charset) throws CharacterCodingException
    {
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
}
