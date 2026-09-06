package com.ruoyi.cost.ai.tool;

/** 受控AI工具统一接口。工具只能通过业务Service/Mapper访问数据，不能执行任意SQL。 */
public interface AiTool<I, O>
{
    String getName();
    O execute(I input);
}
