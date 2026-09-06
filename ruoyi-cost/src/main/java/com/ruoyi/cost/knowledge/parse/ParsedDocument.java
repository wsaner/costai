package com.ruoyi.cost.knowledge.parse;

import java.util.List;

public record ParsedDocument(List<ParsedBlock> blocks, int pageCount)
{
}
