package com.ruoyi.cost.boq.importer;

import java.io.BufferedReader;
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
import com.ruoyi.common.exception.ServiceException;

/** UTF-8/GB18030 CSV全量逐行读取。 */
@Component
public class CsvBoqRowStreamReader implements BoqRowStreamReader
{
    @Override
    public boolean supports(String extension) { return "csv".equalsIgnoreCase(extension); }

    @Override
    public void stream(Path path, String sheetName, int headerRow, BoqRowConsumer consumer) throws Exception
    {
        if (!"CSV".equals(sheetName))
        {
            throw new ServiceException("CSV文件的Sheet名称必须为CSV");
        }
        Charset charset = detectCharset(path);
        char delimiter = detectDelimiter(path, charset);
        try (PushbackReader reader = new PushbackReader(new BufferedReader(Files.newBufferedReader(path, charset)), 1))
        {
            List<String> row;
            int rowIndex = 0;
            while ((row = readRow(reader, delimiter)) != null)
            {
                rowIndex++;
                if (rowIndex <= headerRow)
                {
                    continue;
                }
                Map<Integer, String> values = new LinkedHashMap<>();
                for (int column = 0; column < row.size(); column++)
                {
                    String value = row.get(column).trim();
                    if (!value.isBlank()) values.put(column, value);
                }
                if (!values.isEmpty()) consumer.accept(rowIndex, values);
            }
        }
    }

    private Charset detectCharset(Path path) throws Exception
    {
        byte[] sample;
        try (var input = Files.newInputStream(path)) { sample = input.readNBytes(8192); }
        if (sample.length >= 3 && sample[0] == (byte) 0xEF && sample[1] == (byte) 0xBB && sample[2] == (byte) 0xBF)
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

    private char detectDelimiter(Path path, Charset charset) throws Exception
    {
        char[] candidates = { ',', '\t', ';' };
        int[] scores = new int[candidates.length];
        try (BufferedReader reader = Files.newBufferedReader(path, charset))
        {
            String line;
            int count = 0;
            while (count++ < 20 && (line = reader.readLine()) != null)
            {
                boolean quoted = false;
                for (int p = 0; p < line.length(); p++)
                {
                    if (line.charAt(p) == '"') quoted = !quoted;
                    else if (!quoted)
                    {
                        for (int i = 0; i < candidates.length; i++) if (line.charAt(p) == candidates[i]) scores[i]++;
                    }
                }
            }
        }
        int best = 0;
        for (int i = 1; i < scores.length; i++) if (scores[i] > scores[best]) best = i;
        return candidates[best];
    }

    private List<String> readRow(PushbackReader reader, char delimiter) throws Exception
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
                    if (following == '"') cell.append('"');
                    else { quoted = false; if (following != -1) reader.unread(following); }
                }
                else if (cell.length() == 0) quoted = true;
                else cell.append(value);
            }
            else if (!quoted && value == delimiter) { values.add(cell.toString()); cell.setLength(0); }
            else if (!quoted && (value == '\n' || value == '\r'))
            {
                if (value == '\r')
                {
                    int following = reader.read();
                    if (following != '\n' && following != -1) reader.unread(following);
                }
                values.add(cell.toString());
                return values;
            }
            else cell.append(value);
        }
        if (!readAny && cell.length() == 0 && values.isEmpty()) return null;
        values.add(cell.toString());
        return values;
    }
}
