package com.ruoyi.cost.boq.preview.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.preview.support.SheetSample;
import com.ruoyi.cost.boq.preview.support.WorkbookSample;

/** CSV流式读取，支持 UTF-8/GB18030、引号、换行字段和常见分隔符。 */
@Component
public class CsvStreamingWorkbookReader implements StreamingWorkbookReader
{
    private static final int MAX_ROWS = 100;

    @Override
    public boolean supports(String extension)
    {
        return "csv".equalsIgnoreCase(extension);
    }

    @Override
    public WorkbookSample read(Path path) throws Exception
    {
        Charset charset = detectCharset(path);
        char delimiter = detectDelimiter(path, charset);
        WorkbookSample workbook = new WorkbookSample();
        SheetSample sheet = new SheetSample(0, "CSV");
        workbook.getSheets().add(sheet);
        try (PushbackReader reader = new PushbackReader(
                new BufferedReader(Files.newBufferedReader(path, charset)), 1))
        {
            List<String> row;
            int rowIndex = 0;
            while (rowIndex < MAX_ROWS && (row = readRow(reader, delimiter)) != null)
            {
                Map<Integer, String> values = new LinkedHashMap<>();
                for (int column = 0; column < row.size(); column++)
                {
                    String value = row.get(column);
                    if (rowIndex == 0 && column == 0 && value.startsWith("\uFEFF"))
                    {
                        value = value.substring(1);
                    }
                    if (!value.isBlank())
                    {
                        values.put(column, value.trim());
                    }
                }
                if (!values.isEmpty())
                {
                    sheet.getRows().put(rowIndex, values);
                }
                rowIndex++;
            }
        }
        return workbook;
    }

    private Charset detectCharset(Path path) throws IOException
    {
        byte[] sample;
        try (var input = Files.newInputStream(path))
        {
            sample = input.readNBytes(8192);
        }
        if (sample.length >= 3 && sample[0] == (byte) 0xEF && sample[1] == (byte) 0xBB
                && sample[2] == (byte) 0xBF)
        {
            return StandardCharsets.UTF_8;
        }
        for (int trimmed = 0; trimmed <= 3 && sample.length - trimmed > 0; trimmed++)
        {
            try
            {
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(sample, 0, sample.length - trimmed));
                return StandardCharsets.UTF_8;
            }
            catch (CharacterCodingException ignored) { }
        }
        return Charset.forName("GB18030");
    }

    private char detectDelimiter(Path path, Charset charset) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(path, charset))
        {
            char[] candidates = { ',', '\t', ';' };
            int[] scores = new int[candidates.length];
            String line;
            int sampledLines = 0;
            while (sampledLines < 20 && (line = reader.readLine()) != null)
            {
                for (int i = 0; i < candidates.length; i++)
                {
                    scores[i] += countOutsideQuotes(line, candidates[i]);
                }
                sampledLines++;
            }
            int bestCount = 0;
            char best = ',';
            for (int i = 0; i < candidates.length; i++)
            {
                if (scores[i] > bestCount)
                {
                    bestCount = scores[i];
                    best = candidates[i];
                }
            }
            return best;
        }
    }

    private int countOutsideQuotes(String line, char delimiter)
    {
        int count = 0;
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++)
        {
            char value = line.charAt(i);
            if (value == '"')
            {
                quoted = !quoted;
            }
            else if (!quoted && value == delimiter)
            {
                count++;
            }
        }
        return count;
    }

    private List<String> readRow(PushbackReader reader, char delimiter) throws IOException
    {
        List<String> values = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean readAny = false;
        int next;
        while ((next = reader.read()) != -1)
        {
            readAny = true;
            char value = (char) next;
            if (value == '"')
            {
                if (quoted)
                {
                    int following = reader.read();
                    if (following == '"')
                    {
                        cell.append('"');
                    }
                    else
                    {
                        quoted = false;
                        if (following != -1)
                        {
                            reader.unread(following);
                        }
                    }
                }
                else if (cell.length() == 0)
                {
                    quoted = true;
                }
                else
                {
                    cell.append(value);
                }
            }
            else if (!quoted && value == delimiter)
            {
                values.add(cell.toString());
                cell.setLength(0);
            }
            else if (!quoted && (value == '\n' || value == '\r'))
            {
                if (value == '\r')
                {
                    int following = reader.read();
                    if (following != '\n' && following != -1)
                    {
                        reader.unread(following);
                    }
                }
                values.add(cell.toString());
                return values;
            }
            else
            {
                cell.append(value);
            }
        }
        if (!readAny && cell.length() == 0 && values.isEmpty())
        {
            return null;
        }
        values.add(cell.toString());
        return values;
    }
}
