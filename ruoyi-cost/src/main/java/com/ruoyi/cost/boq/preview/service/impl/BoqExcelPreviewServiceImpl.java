package com.ruoyi.cost.boq.preview.service.impl;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.preview.domain.BoqStandardField;
import com.ruoyi.cost.boq.preview.reader.SpreadsheetReaderFactory;
import com.ruoyi.cost.boq.preview.service.BoqExcelPreviewService;
import com.ruoyi.cost.boq.preview.support.BoqHeaderDetector;
import com.ruoyi.cost.boq.preview.support.DetectedHeader;
import com.ruoyi.cost.boq.preview.support.SheetSample;
import com.ruoyi.cost.boq.preview.support.WorkbookSample;
import com.ruoyi.cost.boq.preview.vo.BoqExcelPreviewVo;
import com.ruoyi.cost.boq.preview.vo.BoqPreviewColumnVo;
import com.ruoyi.cost.boq.preview.vo.BoqSheetPreviewVo;
import com.ruoyi.cost.boq.preview.vo.BoqStandardFieldVo;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;

/** 流式读取、表头检测、确定性字段映射与有限数据预览。 */
@Service
public class BoqExcelPreviewServiceImpl implements BoqExcelPreviewService
{
    private static final Logger log = LoggerFactory.getLogger(BoqExcelPreviewServiceImpl.class);
    private static final String BOQ_FILE_CATEGORY = "BOQ";
    private static final int MAX_PREVIEW_ROWS = 50;
    private static final int MAX_WARNINGS = 20;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("xlsx", "xls", "csv");

    private final ICostProjectFileService projectFileService;
    private final SpreadsheetReaderFactory readerFactory;
    private final BoqHeaderDetector headerDetector;

    public BoqExcelPreviewServiceImpl(ICostProjectFileService projectFileService,
            SpreadsheetReaderFactory readerFactory, BoqHeaderDetector headerDetector)
    {
        this.projectFileService = projectFileService;
        this.readerFactory = readerFactory;
        this.headerDetector = headerDetector;
    }

    @Override
    public BoqExcelPreviewVo uploadAndPreview(Long projectId, MultipartFile file, String operator)
    {
        validatePreviewUpload(file);
        CostProjectFile projectFile = projectFileService.uploadProjectFile(
                projectId, BOQ_FILE_CATEGORY, file, operator);
        return previewProjectFile(projectFile.getId(), null);
    }

    @Override
    public BoqExcelPreviewVo previewProjectFile(Long projectFileId, String sheetName)
    {
        CostProjectFile projectFile = projectFileService.selectProjectFileById(projectFileId);
        String extension = StringUtils.trim(projectFile.getFileExt()).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension))
        {
            throw new ServiceException("仅支持 xlsx、xls、csv 格式的清单预览");
        }
        CostProjectFileDownloadVo download = projectFileService.prepareDownload(projectFileId);
        try
        {
            if ("xlsx".equals(extension) && hasOle2Header(download.getPath()))
            {
                throw new ServiceException("Excel文件已加密或受密码保护，暂不支持解析");
            }
            WorkbookSample workbook = readerFactory.getReader(extension).read(download.getPath());
            return buildPreview(projectFile, workbook, StringUtils.trim(sheetName));
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.warn("工程量清单预览失败，projectFileId={}", projectFileId, e);
            throw new ServiceException("Excel解析失败，请确认文件未损坏、未加密且格式正确")
                    .setDetailMessage(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private BoqExcelPreviewVo buildPreview(CostProjectFile projectFile, WorkbookSample workbook,
            String requestedSheet)
    {
        if (workbook.getSheets().isEmpty())
        {
            throw new ServiceException("Excel中没有可读取的Sheet");
        }
        Map<SheetSample, DetectedHeader> detections = new HashMap<>();
        List<BoqSheetPreviewVo> sheetOptions = new ArrayList<>();
        for (SheetSample sheet : workbook.getSheets())
        {
            DetectedHeader detected = headerDetector.detect(sheet);
            detections.put(sheet, detected);
            BoqSheetPreviewVo option = new BoqSheetPreviewVo();
            option.setIndex(sheet.getIndex());
            option.setName(sheet.getName());
            option.setSampledRowCount(sheet.getRows().size());
            option.setDetectedHeaderRow(detected == null ? null : detected.rowIndex() + 1);
            option.setRecognizedFieldCount(detected == null ? 0 : detected.mappings().size());
            sheetOptions.add(option);
        }

        SheetSample selected = selectSheet(workbook.getSheets(), detections, requestedSheet);
        DetectedHeader header = detections.get(selected);
        if (header == null)
        {
            throw new ServiceException("在Sheet“" + selected.getName()
                    + "”中未找到可识别的工程量清单表头，请确认至少包含项目名称及编码、数量、单价或合价列");
        }

        List<Map<Integer, String>> dataRows = selected.getRows().tailMap(header.rowIndex(), false)
                .values().stream().filter(this::hasValue).limit(MAX_PREVIEW_ROWS).toList();
        if (dataRows.isEmpty())
        {
            throw new ServiceException("检测到表头，但表头后没有有效数据");
        }

        BoqExcelPreviewVo preview = new BoqExcelPreviewVo();
        preview.setProjectFileId(projectFile.getId());
        preview.setFileName(projectFile.getOriginalName());
        preview.setSheets(sheetOptions);
        preview.setSelectedSheet(selected.getName());
        preview.setDetectedHeaderRow(header.rowIndex() + 1);
        preview.setStandardFields(standardFields());
        preview.setColumns(buildColumns(header, dataRows, preview.getWarnings()));
        preview.setPreviewRows(buildRows(preview.getColumns(), dataRows));
        preview.setMappingSuggestions(toColumnKeys(header.mappings()));
        addWarnings(selected, header, dataRows, preview.getWarnings());
        return preview;
    }

    private SheetSample selectSheet(List<SheetSample> sheets, Map<SheetSample, DetectedHeader> detections,
            String requestedSheet)
    {
        if (StringUtils.isNotBlank(requestedSheet))
        {
            return sheets.stream().filter(sheet -> requestedSheet.equals(sheet.getName())).findFirst()
                    .orElseThrow(() -> new ServiceException("指定的Sheet不存在或已被重命名"));
        }
        return sheets.stream().filter(sheet -> detections.get(sheet) != null)
                .max(Comparator.comparingInt(sheet -> detections.get(sheet).score()))
                .orElseThrow(() -> new ServiceException(
                        "未找到可识别的工程量清单表头，请确认文件包含项目名称及编码、数量、单价或合价列"));
    }

    private List<BoqPreviewColumnVo> buildColumns(DetectedHeader header,
            List<Map<Integer, String>> dataRows, List<String> warnings)
    {
        int maxColumn = header.headers().keySet().stream().max(Integer::compareTo).orElse(0);
        for (Map<Integer, String> row : dataRows)
        {
            maxColumn = Math.max(maxColumn, row.keySet().stream().max(Integer::compareTo).orElse(0));
        }
        List<BoqPreviewColumnVo> columns = new ArrayList<>();
        for (int index = 0; index <= maxColumn; index++)
        {
            final int columnIndex = index;
            String headerValue = header.headers().get(index);
            boolean hasData = dataRows.stream().anyMatch(row -> StringUtils.isNotBlank(row.get(columnIndex)));
            if (StringUtils.isBlank(headerValue) && !hasData)
            {
                continue;
            }
            BoqPreviewColumnVo column = new BoqPreviewColumnVo();
            column.setIndex(index);
            column.setKey(CellReference.convertNumToColString(index));
            column.setHeader(StringUtils.isBlank(headerValue) ? "未命名列 " + column.getKey() : headerValue);
            column.setSampleValues(dataRows.stream().map(row -> row.get(columnIndex))
                    .filter(StringUtils::isNotBlank).distinct().limit(3).toList());
            columns.add(column);
            if (StringUtils.isBlank(headerValue) && warnings.size() < MAX_WARNINGS)
            {
                warnings.add("列" + column.getKey() + "没有表头，可能由合并单元格造成，请人工确认映射");
            }
        }
        return columns;
    }

    private List<Map<String, String>> buildRows(List<BoqPreviewColumnVo> columns,
            List<Map<Integer, String>> sourceRows)
    {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map<Integer, String> source : sourceRows)
        {
            Map<String, String> row = new LinkedHashMap<>();
            for (BoqPreviewColumnVo column : columns)
            {
                row.put(column.getKey(), StringUtils.defaultString(source.get(column.getIndex())));
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, String> toColumnKeys(Map<String, Integer> mappings)
    {
        Map<String, String> result = new LinkedHashMap<>();
        mappings.forEach((field, column) -> result.put(field, CellReference.convertNumToColString(column)));
        return result;
    }

    private List<BoqStandardFieldVo> standardFields()
    {
        List<BoqStandardFieldVo> result = new ArrayList<>();
        for (BoqStandardField field : BoqStandardField.values())
        {
            BoqStandardFieldVo option = new BoqStandardFieldVo();
            option.setCode(field.getCode());
            option.setLabel(field.getLabel());
            option.setNumeric(field.isNumeric());
            result.add(option);
        }
        return result;
    }

    private void addWarnings(SheetSample sheet, DetectedHeader header,
            List<Map<Integer, String>> rows, List<String> warnings)
    {
        if (sheet.isMergedCells() && warnings.size() < MAX_WARNINGS)
        {
            warnings.add("Sheet包含合并单元格，系统已按实际有值单元格预览，请重点确认表头映射");
        }
        Map<Integer, String> columnToField = new HashMap<>();
        header.mappings().forEach((field, column) -> columnToField.put(column, field));
        int dataIndex = 0;
        for (Map<Integer, String> row : rows)
        {
            dataIndex++;
            for (Map.Entry<Integer, String> mapping : columnToField.entrySet())
            {
                BoqStandardField field = BoqStandardField.byCode(mapping.getValue());
                String value = row.get(mapping.getKey());
                if (field != null && field.isNumeric() && StringUtils.isNotBlank(value)
                        && !isDecimal(value) && warnings.size() < MAX_WARNINGS)
                {
                    warnings.add("预览第" + dataIndex + "行“" + field.getLabel()
                            + "”不是有效数字：" + abbreviate(value));
                }
            }
        }
    }

    private boolean isDecimal(String raw)
    {
        String value = raw.trim().replace(",", "").replace("￥", "").replace("¥", "")
                .replace(" ", "");
        if (value.isEmpty() || "-".equals(value))
        {
            return true;
        }
        if (value.startsWith("(") && value.endsWith(")"))
        {
            value = "-" + value.substring(1, value.length() - 1);
        }
        try
        {
            new BigDecimal(value);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    private boolean hasValue(Map<Integer, String> row)
    {
        return row.values().stream().anyMatch(StringUtils::isNotBlank);
    }

    private String abbreviate(String value)
    {
        String normalized = value.replaceAll("[\\r\\n\\t]", " ");
        return normalized.length() <= 30 ? normalized : normalized.substring(0, 30) + "...";
    }

    private boolean hasOle2Header(Path path) throws Exception
    {
        byte[] header;
        try (var input = Files.newInputStream(path))
        {
            header = input.readNBytes(8);
        }
        byte[] ole = { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 };
        if (header.length < ole.length)
        {
            return false;
        }
        for (int i = 0; i < ole.length; i++)
        {
            if (header[i] != ole[i])
            {
                return false;
            }
        }
        return true;
    }

    private void validatePreviewUpload(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请选择需要识别的Excel或CSV文件");
        }
        String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension))
        {
            throw new ServiceException("清单字段识别仅支持 xlsx、xls、csv 格式");
        }
    }
}
