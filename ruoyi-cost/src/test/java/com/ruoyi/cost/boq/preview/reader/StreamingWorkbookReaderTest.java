package com.ruoyi.cost.boq.preview.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.cost.boq.preview.support.WorkbookSample;

class StreamingWorkbookReaderTest
{
    @TempDir
    Path tempDir;

    @Test
    void xlsxReaderKeepsOnlyFirstHundredRowsFromLargeSheet() throws Exception
    {
        Path file = tempDir.resolve("large.xlsx");
        SXSSFWorkbook workbook = new SXSSFWorkbook(20);
        try
        {
            var sheet = workbook.createSheet("工程量清单");
            for (int row = 0; row < 5000; row++)
            {
                var excelRow = sheet.createRow(row);
                excelRow.createCell(0).setCellValue(row == 0 ? "项目编码" : "0101" + row);
                excelRow.createCell(1).setCellValue(row == 0 ? "项目名称" : "混凝土");
                if (row == 0)
                {
                    excelRow.createCell(2).setCellValue("工程量");
                    excelRow.createCell(3).setCellValue("综合单价");
                }
                else
                {
                    excelRow.createCell(2).setCellValue(row);
                    excelRow.createCell(3).setCellValue(100.25D);
                }
            }
            try (var output = Files.newOutputStream(file))
            {
                workbook.write(output);
            }
        }
        finally
        {
            workbook.dispose();
            workbook.close();
        }

        WorkbookSample sample = new XlsxStreamingWorkbookReader().read(file);

        assertEquals(1, sample.getSheets().size());
        assertEquals(100, sample.getSheets().get(0).getRows().size());
        assertEquals("项目编码", sample.getSheets().get(0).getRows().get(0).get(0));
    }

    @Test
    void xlsEventReaderReadsValuesAndReportsMergedCells() throws Exception
    {
        Path file = tempDir.resolve("sample.xls");
        try (HSSFWorkbook workbook = new HSSFWorkbook())
        {
            var sheet = workbook.createSheet("清单");
            sheet.createRow(0).createCell(0).setCellValue("工程量清单");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            var header = sheet.createRow(1);
            header.createCell(0).setCellValue("清单编码");
            header.createCell(1).setCellValue("工程名称");
            header.createCell(2).setCellValue("数量");
            header.createCell(3).setCellValue("合价");
            var data = sheet.createRow(2);
            data.createCell(0).setCellValue("010101");
            data.createCell(1).setCellValue("土方");
            data.createCell(2).setCellValue(12.5D);
            data.createCell(3).setCellValue(1000D);
            try (var output = Files.newOutputStream(file))
            {
                workbook.write(output);
            }
        }

        WorkbookSample sample = new XlsStreamingWorkbookReader().read(file);

        assertEquals("清单", sample.getSheets().get(0).getName());
        assertEquals("土方", sample.getSheets().get(0).getRows().get(2).get(1));
        assertTrue(sample.getSheets().get(0).isMergedCells());
    }

    @Test
    void csvReaderSupportsGb18030QuotesAndCommaInsideCell() throws Exception
    {
        Path file = tempDir.resolve("sample.csv");
        String content = "清单编码,工程名称,单位,数量,合价\r\n0101,\"混凝土,泵送\",m3,12.5,1000\r\n";
        Files.write(file, content.getBytes(Charset.forName("GB18030")));

        WorkbookSample sample = new CsvStreamingWorkbookReader().read(file);

        assertEquals("混凝土,泵送", sample.getSheets().get(0).getRows().get(1).get(1));
        assertEquals("12.5", sample.getSheets().get(0).getRows().get(1).get(3));
    }

    @Test
    void csvReaderDetectsSemicolonAfterSingleCellTitleRow() throws Exception
    {
        Path file = tempDir.resolve("semicolon.csv");
        String content = "分部分项工程量清单\n项目编码;项目名称;单位;工程量\n0101;土方;m3;12.5\n";
        Files.writeString(file, content);

        WorkbookSample sample = new CsvStreamingWorkbookReader().read(file);

        assertEquals("项目名称", sample.getSheets().get(0).getRows().get(1).get(1));
        assertEquals("12.5", sample.getSheets().get(0).getRows().get(2).get(3));
    }
}
