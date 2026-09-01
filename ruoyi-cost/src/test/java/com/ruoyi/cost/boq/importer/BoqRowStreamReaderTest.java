package com.ruoyi.cost.boq.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoqRowStreamReaderTest
{
    @TempDir
    Path tempDir;

    @Test
    void csvStreamsAllRowsBeyondPreviewLimit() throws Exception
    {
        Path file = tempDir.resolve("all.csv");
        StringBuilder csv = new StringBuilder("项目编码,项目名称,工程量\n");
        for (int i = 0; i < 100000; i++) csv.append(i).append(",项目").append(i).append(',').append(i).append('\n');
        Files.writeString(file, csv);
        AtomicInteger count = new AtomicInteger();

        new CsvBoqRowStreamReader().stream(file, "CSV", 1, (row, values) -> count.incrementAndGet());

        assertEquals(100000, count.get());
    }

    @Test
    void xlsxStreamsOnlySelectedSheet() throws Exception
    {
        Path file = tempDir.resolve("sheets.xlsx");
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(20))
        {
            workbook.createSheet("说明").createRow(0).createCell(0).setCellValue("忽略");
            var sheet = workbook.createSheet("清单");
            sheet.createRow(0).createCell(0).setCellValue("项目名称");
            for (int i = 1; i <= 150; i++) sheet.createRow(i).createCell(0).setCellValue("项目" + i);
            try (var out = Files.newOutputStream(file)) { workbook.write(out); }
            workbook.dispose();
        }
        AtomicInteger count = new AtomicInteger();

        new XlsxBoqRowStreamReader().stream(file, "清单", 1, (row, values) -> count.incrementAndGet());

        assertEquals(150, count.get());
    }

    @Test
    void xlsStreamsRowsAfterHeader() throws Exception
    {
        Path file = tempDir.resolve("sample.xls");
        try (HSSFWorkbook workbook = new HSSFWorkbook())
        {
            var sheet = workbook.createSheet("清单");
            sheet.createRow(0).createCell(0).setCellValue("项目名称");
            sheet.createRow(1).createCell(0).setCellValue("土方");
            sheet.createRow(2).createCell(0).setCellValue("混凝土");
            try (var out = Files.newOutputStream(file)) { workbook.write(out); }
        }
        AtomicInteger count = new AtomicInteger();

        new XlsBoqRowStreamReader().stream(file, "清单", 1, (row, values) -> count.incrementAndGet());

        assertEquals(2, count.get());
    }
}
