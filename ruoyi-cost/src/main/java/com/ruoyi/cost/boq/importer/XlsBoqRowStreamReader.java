package com.ruoyi.cost.boq.importer;

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

/** XLS HSSF事件模型全量逐行读取。 */
@Component
public class XlsBoqRowStreamReader implements BoqRowStreamReader
{
    @Override
    public boolean supports(String extension)
    {
        return "xls".equalsIgnoreCase(extension);
    }

    @Override
    public void stream(Path path, String sheetName, int headerRow, BoqRowConsumer consumer) throws Exception
    {
        Listener listener = new Listener(sheetName, headerRow, consumer);
        HSSFRequest request = new HSSFRequest();
        request.addListenerForAllRecords(listener);
        try (POIFSFileSystem fileSystem = new POIFSFileSystem(Files.newInputStream(path)))
        {
            new HSSFEventFactory().processWorkbookEvents(request, fileSystem);
        }
        listener.finish();
        if (!listener.isFound())
        {
            throw new ServiceException("指定的Sheet不存在或已被重命名");
        }
    }

    private static class Listener implements HSSFListener
    {
        private final String targetSheet;
        private final int headerRow;
        private final BoqRowConsumer consumer;
        private final List<String> sheetNames = new ArrayList<>();
        private SSTRecord strings;
        private int sheetIndex = -1;
        private boolean selected;
        private boolean found;
        private Integer currentRow;
        private Map<Integer, String> currentValues = new LinkedHashMap<>();
        private Integer pendingFormulaRow;
        private Integer pendingFormulaColumn;

        Listener(String targetSheet, int headerRow, BoqRowConsumer consumer)
        {
            this.targetSheet = targetSheet;
            this.headerRow = headerRow;
            this.consumer = consumer;
        }

        @Override
        public void processRecord(Record record)
        {
            if (record instanceof FilePassRecord)
            {
                throw new ServiceException("Excel文件已加密或受密码保护，暂不支持导入");
            }
            if (record instanceof BoundSheetRecord sheet)
            {
                sheetNames.add(sheet.getSheetname());
            }
            else if (record instanceof SSTRecord sst)
            {
                strings = sst;
            }
            else if (record instanceof BOFRecord bof && bof.getType() == BOFRecord.TYPE_WORKSHEET)
            {
                flush();
                sheetIndex++;
                String name = sheetIndex < sheetNames.size() ? sheetNames.get(sheetIndex) : "Sheet" + (sheetIndex + 1);
                selected = targetSheet.equals(name);
                found |= selected;
            }
            else if (selected)
            {
                processCell(record);
            }
        }

        private void processCell(Record record)
        {
            if (record instanceof LabelSSTRecord cell && strings != null)
            {
                put(cell.getRow(), cell.getColumn(), strings.getString(cell.getSSTIndex()).toString());
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
                if (cell.getCachedResultTypeEnum() == CellType.STRING)
                {
                    pendingFormulaRow = cell.getRow();
                    pendingFormulaColumn = (int) cell.getColumn();
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
            if (currentRow == null || currentRow != row)
            {
                flush();
                currentRow = row;
            }
            if (row + 1 > headerRow)
            {
                currentValues.put(column, value == null ? "" : value.trim());
            }
        }

        private void flush()
        {
            if (selected && currentRow != null && currentRow + 1 > headerRow
                    && currentValues.values().stream().anyMatch(value -> !value.isBlank()))
            {
                consumer.accept(currentRow + 1, new LinkedHashMap<>(currentValues));
            }
            currentRow = null;
            currentValues.clear();
        }

        void finish() { flush(); }
        boolean isFound() { return found; }
    }
}
