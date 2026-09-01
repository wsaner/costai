package com.ruoyi.cost.boq.preview.reader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FilePassRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.MergeCellsRecord;
import org.apache.poi.hssf.record.MulRKRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.preview.support.SheetSample;
import com.ruoyi.cost.boq.preview.support.WorkbookSample;

/** XLS事件模型读取：完整顺序扫描，但每个Sheet只保留前100行。 */
@Component
public class XlsStreamingWorkbookReader implements StreamingWorkbookReader
{
    private static final int MAX_SAMPLE_ROW_INDEX = 99;

    @Override
    public boolean supports(String extension)
    {
        return "xls".equalsIgnoreCase(extension);
    }

    @Override
    public WorkbookSample read(Path path) throws Exception
    {
        WorkbookSample workbook = new WorkbookSample();
        XlsListener listener = new XlsListener(workbook);
        HSSFRequest request = new HSSFRequest();
        request.addListenerForAllRecords(listener);
        try (POIFSFileSystem fileSystem = new POIFSFileSystem(Files.newInputStream(path)))
        {
            new HSSFEventFactory().processWorkbookEvents(request, fileSystem);
        }
        return workbook;
    }

    private static class XlsListener implements HSSFListener
    {
        private final WorkbookSample workbook;
        private final List<String> sheetNames = new ArrayList<>();
        private SSTRecord sharedStrings;
        private SheetSample currentSheet;
        private int sheetIndex = -1;
        private Integer pendingFormulaRow;
        private Integer pendingFormulaColumn;

        XlsListener(WorkbookSample workbook)
        {
            this.workbook = workbook;
        }

        @Override
        public void processRecord(Record record)
        {
            if (record instanceof FilePassRecord)
            {
                throw new ServiceException("Excel文件已加密或受密码保护，暂不支持解析");
            }
            if (record instanceof BoundSheetRecord boundSheet)
            {
                sheetNames.add(boundSheet.getSheetname());
            }
            else if (record instanceof SSTRecord sst)
            {
                sharedStrings = sst;
            }
            else if (record instanceof BOFRecord bof && bof.getType() == BOFRecord.TYPE_WORKSHEET)
            {
                sheetIndex++;
                String name = sheetIndex < sheetNames.size() ? sheetNames.get(sheetIndex) : "Sheet" + (sheetIndex + 1);
                currentSheet = new SheetSample(sheetIndex, name);
                workbook.getSheets().add(currentSheet);
            }
            else if (currentSheet != null)
            {
                processCellRecord(record);
            }
        }

        private void processCellRecord(Record record)
        {
            if (record instanceof MergeCellsRecord)
            {
                currentSheet.setMergedCells(true);
            }
            else if (record instanceof LabelSSTRecord cell && sharedStrings != null)
            {
                put(cell.getRow(), cell.getColumn(), sharedStrings.getString(cell.getSSTIndex()).toString());
            }
            else if (record instanceof LabelRecord cell)
            {
                put(cell.getRow(), cell.getColumn(), cell.getValue());
            }
            else if (record instanceof NumberRecord cell)
            {
                put(cell.getRow(), cell.getColumn(), NumberToTextConverter.toText(cell.getValue()));
            }
            else if (record instanceof RKRecord cell)
            {
                put(cell.getRow(), cell.getColumn(), NumberToTextConverter.toText(cell.getRKNumber()));
            }
            else if (record instanceof MulRKRecord cells)
            {
                for (int i = 0; i < cells.getNumColumns(); i++)
                {
                    put(cells.getRow(), cells.getFirstColumn() + i,
                            NumberToTextConverter.toText(cells.getRKNumberAt(i)));
                }
            }
            else if (record instanceof BoolErrRecord cell)
            {
                put(cell.getRow(), cell.getColumn(), cell.isBoolean()
                        ? Boolean.toString(cell.getBooleanValue()) : "#ERROR");
            }
            else if (record instanceof FormulaRecord cell)
            {
                CellType type = cell.getCachedResultTypeEnum();
                if (type == CellType.STRING)
                {
                    pendingFormulaRow = cell.getRow();
                    pendingFormulaColumn = (int) cell.getColumn();
                }
                else if (type == CellType.BOOLEAN)
                {
                    put(cell.getRow(), cell.getColumn(), Boolean.toString(cell.getCachedBooleanValue()));
                }
                else if (type == CellType.ERROR)
                {
                    put(cell.getRow(), cell.getColumn(), "#ERROR");
                }
                else
                {
                    put(cell.getRow(), cell.getColumn(), NumberToTextConverter.toText(cell.getValue()));
                }
            }
            else if (record instanceof StringRecord value && pendingFormulaRow != null)
            {
                put(pendingFormulaRow, pendingFormulaColumn, value.getString());
                pendingFormulaRow = null;
                pendingFormulaColumn = null;
            }
        }

        private void put(int row, int column, String value)
        {
            if (row > MAX_SAMPLE_ROW_INDEX)
            {
                return;
            }
            Map<Integer, String> values = currentSheet.getRows().computeIfAbsent(row,
                    ignored -> new LinkedHashMap<>());
            values.put(column, value == null ? "" : value.trim());
        }
    }
}
