package com.ruoyi.cost.boq.importer;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import com.ruoyi.common.exception.ServiceException;

/** XLSX SAX全量逐行读取。 */
@Component
public class XlsxBoqRowStreamReader implements BoqRowStreamReader
{
    @Override
    public boolean supports(String extension)
    {
        return "xlsx".equalsIgnoreCase(extension);
    }

    @Override
    public void stream(Path path, String sheetName, int headerRow, BoqRowConsumer consumer) throws Exception
    {
        boolean found = false;
        try (OPCPackage pkg = OPCPackage.open(path.toFile(), PackageAccess.READ))
        {
            XSSFReader reader = new XSSFReader(pkg, true);
            StylesTable styles = reader.getStylesTable();
            SharedStrings strings = reader.getSharedStringsTable();
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (sheets.hasNext())
            {
                try (InputStream input = sheets.next())
                {
                    if (!sheetName.equals(sheets.getSheetName()))
                    {
                        continue;
                    }
                    found = true;
                    XMLReader parser = SAXHelper.newXMLReader();
                    parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings,
                            new Handler(headerRow, consumer), new DataFormatter(), false));
                    parser.parse(new InputSource(input));
                }
            }
        }
        if (!found)
        {
            throw new ServiceException("指定的Sheet不存在或已被重命名");
        }
    }

    private static class Handler implements XSSFSheetXMLHandler.SheetContentsHandler
    {
        private final int headerRow;
        private final BoqRowConsumer consumer;
        private int rowIndex;
        private Map<Integer, String> current;

        Handler(int headerRow, BoqRowConsumer consumer)
        {
            this.headerRow = headerRow;
            this.consumer = consumer;
        }

        @Override
        public void startRow(int rowNum)
        {
            rowIndex = rowNum;
            current = new LinkedHashMap<>();
        }

        @Override
        public void endRow(int rowNum)
        {
            if (rowNum + 1 > headerRow && current.values().stream().anyMatch(value -> !value.isBlank()))
            {
                consumer.accept(rowNum + 1, current);
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment)
        {
            if (rowIndex + 1 > headerRow && cellReference != null)
            {
                current.put((int) new CellReference(cellReference).getCol(),
                        formattedValue == null ? "" : formattedValue.trim());
            }
        }
    }
}
