package com.ruoyi.cost.boq.preview.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.preview.domain.BoqStandardField;
import com.ruoyi.cost.boq.preview.service.BoqColumnMappingService;

/** 关键词归一化 + Levenshtein 相似度的首版字段映射。 */
@Service
public class RuleBasedBoqColumnMappingService implements BoqColumnMappingService
{
    private static final double SIMILARITY_THRESHOLD = 0.72D;
    private static final Pattern UNIT_SUFFIX = Pattern.compile("[（(][^）)]*[）)]");
    private static final Pattern NOISE = Pattern.compile("[\\s\\-_/：:、，,。.\\[\\]【】]");

    @Override
    public Map<String, Integer> suggest(Map<Integer, String> headers)
    {
        List<Candidate> candidates = new ArrayList<>();
        for (Map.Entry<Integer, String> column : headers.entrySet())
        {
            String normalizedHeader = normalize(column.getValue());
            if (StringUtils.isBlank(normalizedHeader))
            {
                continue;
            }
            for (BoqStandardField field : BoqStandardField.values())
            {
                double score = bestScore(normalizedHeader, field);
                if (score >= SIMILARITY_THRESHOLD)
                {
                    candidates.add(new Candidate(field, column.getKey(), score));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparingInt(Candidate::column));

        Map<String, Integer> result = new LinkedHashMap<>();
        Set<Integer> usedColumns = new HashSet<>();
        for (Candidate candidate : candidates)
        {
            if (!result.containsKey(candidate.field().getCode()) && usedColumns.add(candidate.column()))
            {
                result.put(candidate.field().getCode(), candidate.column());
            }
        }
        return result;
    }

    String normalize(String text)
    {
        if (text == null)
        {
            return "";
        }
        String value = UNIT_SUFFIX.matcher(text).replaceAll("");
        value = NOISE.matcher(value).replaceAll("");
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private double bestScore(String header, BoqStandardField field)
    {
        double best = 0D;
        for (String aliasValue : field.getAliases())
        {
            String alias = normalize(aliasValue);
            if (header.equals(alias))
            {
                return 1D;
            }
            if (Math.min(header.length(), alias.length()) >= 2
                    && (header.contains(alias) || alias.contains(header)))
            {
                best = Math.max(best, 0.88D + 0.1D * Math.min(header.length(), alias.length())
                        / Math.max(header.length(), alias.length()));
            }
            best = Math.max(best, similarity(header, alias));
        }
        return best;
    }

    private double similarity(String left, String right)
    {
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0)
        {
            return 1D;
        }
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++)
        {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++)
        {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++)
            {
                int substitution = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return 1D - (double) previous[right.length()] / maxLength;
    }

    private record Candidate(BoqStandardField field, int column, double score) { }
}
