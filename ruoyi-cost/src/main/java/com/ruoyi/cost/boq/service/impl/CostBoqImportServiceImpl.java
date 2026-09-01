package com.ruoyi.cost.boq.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqImportError;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.dto.CostBoqImportRequest;
import com.ruoyi.cost.boq.importer.BoqRowStreamReaderFactory;
import com.ruoyi.cost.boq.preview.domain.BoqStandardField;
import com.ruoyi.cost.boq.service.CostBoqImportService;
import com.ruoyi.cost.boq.vo.CostBoqImportResultVo;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;
import com.ruoyi.cost.project.service.ICostProjectService;

/** 用户确认字段映射后的全量流式导入。 */
@Service
public class CostBoqImportServiceImpl implements CostBoqImportService
{
    private static final Logger log = LoggerFactory.getLogger(CostBoqImportServiceImpl.class);
    private static final int CHUNK_SIZE = 500;
    private static final Pattern COLUMN_KEY = Pattern.compile("^[A-Z]{1,3}$");
    private static final Set<String> BUSINESS_TYPES = Set.of(
            "BOQ", "CONTROL_PRICE", "BID_PRICE", "SUBMITTED", "REVIEWED", "SETTLEMENT", "OTHER");

    private final ICostProjectService projectService;
    private final ICostProjectFileService fileService;
    private final BoqRowStreamReaderFactory readerFactory;
    private final CostBoqImportPersistenceService persistence;
    private final ObjectMapper objectMapper;

    public CostBoqImportServiceImpl(ICostProjectService projectService, ICostProjectFileService fileService,
            BoqRowStreamReaderFactory readerFactory, CostBoqImportPersistenceService persistence,
            ObjectMapper objectMapper)
    {
        this.projectService = projectService;
        this.fileService = fileService;
        this.readerFactory = readerFactory;
        this.persistence = persistence;
        this.objectMapper = objectMapper;
    }

    @Override
    public CostBoqImportResultVo importBoq(Long projectId, CostBoqImportRequest request, String operator)
    {
        projectService.selectCostProjectById(projectId);
        CostProjectFile projectFile = fileService.selectProjectFileById(request.getProjectFileId());
        if (!projectId.equals(projectFile.getProjectId()))
        {
            throw new ServiceException("所选文件不属于当前项目");
        }
        validateBusinessType(request.getBusinessType());
        Map<Integer, BoqStandardField> mappings = validateMappings(request.getColumnMappings());
        CostProjectFileDownloadVo download = fileService.prepareDownload(projectFile.getId());
        String extension = StringUtils.trim(projectFile.getFileExt()).toLowerCase(Locale.ROOT);

        CostBoqBatch batch = buildBatch(projectId, projectFile.getId(), request, operator);
        Long batchId = persistence.createBatch(batch);
        ImportAccumulator accumulator = new ImportAccumulator(projectId, batchId, request,
                mappings, operator, persistence, objectMapper);
        try
        {
            readerFactory.getReader(extension).stream(download.getPath(), request.getSheetName(),
                    request.getHeaderRow(), accumulator::accept);
            accumulator.flush();
            String status = accumulator.status();
            String summary = accumulator.totalCount == 0 ? "表头后没有有效数据" : null;
            persistence.finish(batchId, accumulator.totalCount, accumulator.successCount,
                    accumulator.failCount, accumulator.totalAmount, status, summary, operator);
            return accumulator.result(status);
        }
        catch (Exception e)
        {
            accumulator.safeFlush();
            String message = safeMessage(e);
            String status = accumulator.successCount > 0 ? "PARTIAL_FAILED" : "FAILED";
            try
            {
                persistence.finish(batchId, accumulator.totalCount, accumulator.successCount,
                        accumulator.failCount, accumulator.totalAmount, status, message, operator);
            }
            catch (Exception finishError)
            {
                log.error("清单导入失败且批次状态更新失败，batchId={}", batchId, finishError);
            }
            log.warn("清单导入中断，batchId={}", batchId, e);
            throw new ServiceException("清单导入失败，批次已保留，可查看失败原因：" + message);
        }
    }

    private CostBoqBatch buildBatch(Long projectId, Long fileId, CostBoqImportRequest request, String operator)
    {
        CostBoqBatch batch = new CostBoqBatch();
        batch.setProjectId(projectId);
        batch.setBatchName(StringUtils.trim(request.getBatchName()));
        batch.setBusinessType(request.getBusinessType());
        batch.setSourceFileId(fileId);
        batch.setSheetName(request.getSheetName());
        batch.setHeaderRow(request.getHeaderRow());
        batch.setProfessionalType(StringUtils.trimToNull(request.getProfessionalType()));
        batch.setImportStatus("IMPORTING");
        batch.setCreateBy(operator);
        batch.setCreateTime(DateUtils.getNowDate());
        try
        {
            batch.setFieldMappingJson(objectMapper.writeValueAsString(request.getColumnMappings()));
        }
        catch (Exception e)
        {
            throw new ServiceException("字段映射序列化失败");
        }
        return batch;
    }

    private Map<Integer, BoqStandardField> validateMappings(Map<String, String> source)
    {
        Map<Integer, BoqStandardField> result = new LinkedHashMap<>();
        Set<BoqStandardField> usedFields = new HashSet<>();
        if (source == null)
        {
            throw new ServiceException("字段映射不能为空");
        }
        source.forEach((rawColumn, fieldCode) -> {
            String column = StringUtils.trim(rawColumn).toUpperCase(Locale.ROOT);
            BoqStandardField field = BoqStandardField.byCode(StringUtils.trim(fieldCode));
            if (!COLUMN_KEY.matcher(column).matches() || field == null)
            {
                throw new ServiceException("字段映射包含无效列或标准字段");
            }
            int index;
            try
            {
                index = CellReference.convertColStringToIndex(column);
            }
            catch (IllegalArgumentException e)
            {
                throw new ServiceException("Excel列标识无效：" + column);
            }
            if (index > 16383 || result.put(index, field) != null || !usedFields.add(field))
            {
                throw new ServiceException("同一Excel列或标准字段不能重复映射");
            }
        });
        if (!usedFields.contains(BoqStandardField.ITEM_NAME))
        {
            throw new ServiceException("必须映射项目名称");
        }
        return result;
    }

    private void validateBusinessType(String businessType)
    {
        if (!BUSINESS_TYPES.contains(businessType))
        {
            throw new ServiceException("清单业务类型无效");
        }
    }

    private String safeMessage(Exception e)
    {
        String message = e instanceof ServiceException ? e.getMessage() : "文件损坏或格式无法读取";
        return StringUtils.isBlank(message) ? "未知解析错误" : StringUtils.substring(message, 0, 500);
    }

    private static class ImportAccumulator
    {
        private final Long projectId;
        private final Long batchId;
        private final CostBoqImportRequest request;
        private final Map<Integer, BoqStandardField> mappings;
        private final String operator;
        private final CostBoqImportPersistenceService persistence;
        private final ObjectMapper objectMapper;
        private final List<CostBoqItem> items = new ArrayList<>();
        private final List<CostBoqImportError> errors = new ArrayList<>();
        private int totalCount;
        private int successCount;
        private int failCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int pendingSuccessCount;
        private int pendingFailCount;
        private BigDecimal pendingTotalAmount = BigDecimal.ZERO;

        ImportAccumulator(Long projectId, Long batchId, CostBoqImportRequest request,
                Map<Integer, BoqStandardField> mappings, String operator,
                CostBoqImportPersistenceService persistence, ObjectMapper objectMapper)
        {
            this.projectId = projectId;
            this.batchId = batchId;
            this.request = request;
            this.mappings = mappings;
            this.operator = operator;
            this.persistence = persistence;
            this.objectMapper = objectMapper;
        }

        void accept(int sourceRow, Map<Integer, String> source)
        {
            Map<String, String> values = new LinkedHashMap<>();
            mappings.forEach((column, field) -> values.put(field.getCode(), clean(source.get(column))));
            if (values.values().stream().allMatch(StringUtils::isBlank))
            {
                return;
            }
            totalCount++;
            try
            {
                CostBoqItem item = toItem(sourceRow, values);
                items.add(item);
                pendingSuccessCount++;
                if (item.getTotalPrice() != null)
                {
                    pendingTotalAmount = pendingTotalAmount.add(item.getTotalPrice());
                }
            }
            catch (RowException e)
            {
                errors.add(toError(sourceRow, values, e));
                pendingFailCount++;
            }
            if (items.size() + errors.size() >= CHUNK_SIZE) flush();
        }

        private CostBoqItem toItem(int sourceRow, Map<String, String> values)
        {
            String itemName = requiredText(values, "itemName", "项目名称", 500);
            CostBoqItem item = new CostBoqItem();
            item.setProjectId(projectId);
            item.setBatchId(batchId);
            item.setSequenceNo(text(values, "sequenceNo", "序号", 64));
            item.setItemCode(text(values, "itemCode", "项目编码", 100));
            item.setItemName(itemName);
            item.setItemFeature(text(values, "itemFeature", "项目特征", 2000));
            item.setUnit(text(values, "unit", "单位", 50));
            item.setQuantity(decimal(values, "quantity", "工程量", 24, 8));
            item.setUnitPrice(decimal(values, "unitPrice", "综合单价", 24, 8));
            item.setTotalPrice(decimal(values, "totalPrice", "合价", 24, 6));
            item.setLaborPrice(decimal(values, "laborPrice", "人工费", 24, 6));
            item.setMaterialPrice(decimal(values, "materialPrice", "材料费", 24, 6));
            item.setMachinePrice(decimal(values, "machinePrice", "机械费", 24, 6));
            item.setManagementFee(decimal(values, "managementFee", "管理费", 24, 6));
            item.setProfit(decimal(values, "profit", "利润", 24, 6));
            item.setTax(decimal(values, "tax", "税金", 24, 6));
            if (item.getQuantity() != null && item.getUnitPrice() != null)
            {
                item.setCalculatedTotalPrice(item.getQuantity().multiply(item.getUnitPrice())
                        .setScale(6, RoundingMode.HALF_UP));
            }
            item.setProfessionalType(StringUtils.trimToNull(request.getProfessionalType()));
            item.setSourceSheet(request.getSheetName());
            item.setSourceRow(sourceRow);
            item.setCreateBy(operator);
            item.setCreateTime(new Date());
            return item;
        }

        private String requiredText(Map<String, String> values, String code, String label, int max)
        {
            String value = text(values, code, label, max);
            if (StringUtils.isBlank(value)) throw new RowException(code, value, label + "不能为空");
            return value;
        }

        private String text(Map<String, String> values, String code, String label, int max)
        {
            String value = clean(values.get(code));
            if (value != null && value.length() > max)
            {
                throw new RowException(code, value, label + "长度不能超过" + max + "个字符");
            }
            return value;
        }

        private BigDecimal decimal(Map<String, String> values, String code, String label,
                int precision, int scale)
        {
            String raw = clean(values.get(code));
            if (StringUtils.isBlank(raw) || "-".equals(raw)) return null;
            String normalized = raw.replace(",", "").replace("￥", "").replace("¥", "").replace(" ", "");
            if (normalized.startsWith("(") && normalized.endsWith(")"))
            {
                normalized = "-" + normalized.substring(1, normalized.length() - 1);
            }
            try
            {
                BigDecimal value = new BigDecimal(normalized);
                if (Math.max(0, value.scale()) > scale || value.precision() - Math.min(value.scale(), 0) > precision)
                {
                    throw new RowException(code, raw, label + "超出允许精度");
                }
                return value;
            }
            catch (NumberFormatException e)
            {
                throw new RowException(code, raw, label + "不是有效数字");
            }
        }

        private CostBoqImportError toError(int sourceRow, Map<String, String> values, RowException cause)
        {
            CostBoqImportError error = new CostBoqImportError();
            error.setProjectId(projectId);
            error.setBatchId(batchId);
            error.setSourceSheet(request.getSheetName());
            error.setSourceRow(sourceRow);
            error.setErrorField(cause.field);
            error.setRawValue(StringUtils.substring(cause.rawValue, 0, 1000));
            error.setErrorMessage(cause.getMessage());
            try { error.setRawDataJson(objectMapper.writeValueAsString(values)); }
            catch (Exception ignored) { error.setRawDataJson("{}"); }
            error.setCreateBy(operator);
            error.setCreateTime(new Date());
            return error;
        }

        void flush()
        {
            if (items.isEmpty() && errors.isEmpty()) return;
            persistence.persistChunk(new ArrayList<>(items), new ArrayList<>(errors));
            successCount += pendingSuccessCount;
            failCount += pendingFailCount;
            totalAmount = totalAmount.add(pendingTotalAmount);
            items.clear();
            errors.clear();
            pendingSuccessCount = 0;
            pendingFailCount = 0;
            pendingTotalAmount = BigDecimal.ZERO;
        }

        void safeFlush()
        {
            try { flush(); }
            catch (Exception ignored)
            {
                items.clear();
                errors.clear();
                pendingSuccessCount = 0;
                pendingFailCount = 0;
                pendingTotalAmount = BigDecimal.ZERO;
            }
        }

        String status()
        {
            if (successCount == 0) return "FAILED";
            return failCount == 0 ? "SUCCESS" : "PARTIAL_FAILED";
        }

        CostBoqImportResultVo result(String status)
        {
            CostBoqImportResultVo result = new CostBoqImportResultVo();
            result.setBatchId(batchId);
            result.setImportStatus(status);
            result.setTotalCount(totalCount);
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setTotalAmount(totalAmount);
            return result;
        }

        private static String clean(String value)
        {
            return StringUtils.isBlank(value) ? null : value.trim();
        }
    }

    private static class RowException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private final String field;
        private final String rawValue;

        RowException(String field, String rawValue, String message)
        {
            super(message);
            this.field = field;
            this.rawValue = rawValue;
        }
    }
}
