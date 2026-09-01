package com.ruoyi.cost.boq.preview.reader;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import com.ruoyi.cost.boq.preview.support.SheetSample;
import com.ruoyi.cost.boq.preview.support.WorkbookSample;

/** XLSX SAX读取：每个Sheet只保留前100个物理行。 */
@Component
public class XlsxStreamingWorkbookReader implements StreamingWorkbookReader
{
    public static final int MAX_SAMPLE_ROW_INDEX = 99;

    @Override
    public boolean supports(String extension)
    {
        return "xlsx".equalsIgnoreCase(extension);
    }

    @Override
    public WorkbookSample read(Path path) throws Exception
    {
        WorkbookSample workbook = new WorkbookSample();
        try (OPCPackage pkg = OPCPackage.open(path.toFile(), PackageAccess.READ))
        {
            XSSFReader reader = new XSSFReader(pkg, true);
            StylesTable styles = reader.getStylesTable();
            SharedStrings sharedStrings = reader.getSharedStringsTable();
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            int sheetIndex = 0;
            while (sheets.hasNext())
            {
                try (InputStream sheetStream = sheets.next())
                {
                    SheetSample sheet = new SheetSample(sheetIndex++, sheets.getSheetName());
                    workbook.getSheets().add(sheet);
                    parseSheet(styles, sharedStrings, sheetStream, sheet);
                }
            }
        }
        return workbook;
    }

    private void parseSheet(StylesTable styles, SharedStrings sharedStrings, InputStream input,
            SheetSample sheet) throws Exception
    {
        XMLReader parser = SAXHelper.newXMLReader();
        XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(styles, sharedStrings,
                new SampleSheetHandler(sheet), new DataFormatter(), false);
        parser.setContentHandler(handler);
        try
        {
            parser.parse(new InputSource(input));
        }
        catch (PreviewLimitReachedException ignored)
        {
            // 预览只需要前100个物理行，主动停止当前Sheet的SAX解析。
        }
    }

    private static class SampleSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler
    {
        private final SheetSample sheet;
        private int rowIndex;
        private Map<Integer, String> currentRow;

        SampleSheetHandler(SheetSample sheet)
        {
            this.sheet = sheet;
        }

        @Override
        public void startRow(int rowNum)
        {
            rowIndex = rowNum;
            currentRow = new LinkedHashMap<>();
        }

        @Override
        public void endRow(int rowNum)
        {
            if (rowNum <= MAX_SAMPLE_ROW_INDEX && !currentRow.isEmpty())
            {
                sheet.getRows().put(rowNum, currentRow);
            }
            if (rowNum >= MAX_SAMPLE_ROW_INDEX)
            {
                throw new PreviewLimitReachedException();
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment)
        {
            if (rowIndex <= MAX_SAMPLE_ROW_INDEX && cellReference != null)
            {
                int column = new CellReference(cellReference).getCol();
                currentRow.put(column, formattedValue == null ? "" : formattedValue.trim());
            }
        }
    }

    private static class PreviewLimitReachedException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
    }
}
