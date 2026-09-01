package com.ruoyi.cost.boq.preview.domain;

import java.util.List;

/** 工程量清单预览阶段支持的标准字段。 */
public enum BoqStandardField
{
    SEQUENCE_NO("sequenceNo", "序号", false, List.of("序号", "序列号", "行号")),
    ITEM_CODE("itemCode", "项目编码", false, List.of("项目编码", "清单编码", "清单编号", "编号", "编码")),
    ITEM_NAME("itemName", "项目名称", false, List.of("项目名称", "工程名称", "清单名称", "工程项目名称", "名称")),
    ITEM_FEATURE("itemFeature", "项目特征", false, List.of("项目特征", "项目特征描述", "特征描述", "清单特征", "特征")),
    UNIT("unit", "计量单位", false, List.of("计量单位", "单位")),
    QUANTITY("quantity", "工程量", true, List.of("工程量", "数量", "工程数量")),
    UNIT_PRICE("unitPrice", "综合单价", true, List.of("综合单价", "单价", "综合价", "含税单价")),
    TOTAL_PRICE("totalPrice", "合价", true, List.of("综合合价", "合价", "总价", "金额", "总金额")),
    LABOR_PRICE("laborPrice", "人工费", true, List.of("人工费", "人工单价", "人工费合计")),
    MATERIAL_PRICE("materialPrice", "材料费", true, List.of("材料费", "材料单价", "材料费合计")),
    MACHINE_PRICE("machinePrice", "机械费", true, List.of("机械费", "机械单价", "机械费合计")),
    MANAGEMENT_FEE("managementFee", "管理费", true, List.of("管理费", "企业管理费")),
    PROFIT("profit", "利润", true, List.of("利润", "利润金额")),
    TAX("tax", "税金", true, List.of("税金", "税费", "增值税"));

    private final String code;
    private final String label;
    private final boolean numeric;
    private final List<String> aliases;

    BoqStandardField(String code, String label, boolean numeric, List<String> aliases)
    {
        this.code = code;
        this.label = label;
        this.numeric = numeric;
        this.aliases = aliases;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public boolean isNumeric() { return numeric; }
    public List<String> getAliases() { return aliases; }

    public static BoqStandardField byCode(String code)
    {
        for (BoqStandardField field : values())
        {
            if (field.code.equals(code))
            {
                return field;
            }
        }
        return null;
    }
}
