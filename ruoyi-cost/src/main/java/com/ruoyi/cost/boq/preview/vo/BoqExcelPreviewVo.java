package com.ruoyi.cost.boq.preview.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Excel/CSV 工程量清单预览结果。 */
public class BoqExcelPreviewVo
{
    private Long projectFileId;
    private String fileName;
    private List<BoqSheetPreviewVo> sheets = new ArrayList<>();
    private String selectedSheet;
    private int detectedHeaderRow;
    private List<BoqPreviewColumnVo> columns = new ArrayList<>();
    private List<Map<String, String>> previewRows = new ArrayList<>();
    private Map<String, String> mappingSuggestions = new LinkedHashMap<>();
    private List<BoqStandardFieldVo> standardFields = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public Long getProjectFileId() { return projectFileId; }
    public void setProjectFileId(Long projectFileId) { this.projectFileId = projectFileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public List<BoqSheetPreviewVo> getSheets() { return sheets; }
    public void setSheets(List<BoqSheetPreviewVo> sheets) { this.sheets = sheets; }
    public String getSelectedSheet() { return selectedSheet; }
    public void setSelectedSheet(String selectedSheet) { this.selectedSheet = selectedSheet; }
    public int getDetectedHeaderRow() { return detectedHeaderRow; }
    public void setDetectedHeaderRow(int detectedHeaderRow) { this.detectedHeaderRow = detectedHeaderRow; }
    public List<BoqPreviewColumnVo> getColumns() { return columns; }
    public void setColumns(List<BoqPreviewColumnVo> columns) { this.columns = columns; }
    public List<Map<String, String>> getPreviewRows() { return previewRows; }
    public void setPreviewRows(List<Map<String, String>> previewRows) { this.previewRows = previewRows; }
    public Map<String, String> getMappingSuggestions() { return mappingSuggestions; }
    public void setMappingSuggestions(Map<String, String> mappingSuggestions) { this.mappingSuggestions = mappingSuggestions; }
    public List<BoqStandardFieldVo> getStandardFields() { return standardFields; }
    public void setStandardFields(List<BoqStandardFieldVo> standardFields) { this.standardFields = standardFields; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
