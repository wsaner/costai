package com.ruoyi.cost.boq.match.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/** 清单名称、特征、编码和单位的确定性标准化。 */
@Component
public class BoqTextNormalizer
{
    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
            Map.entry("平方米", "m2"), Map.entry("平方公尺", "m2"), Map.entry("m2", "m2"),
            Map.entry("立方米", "m3"), Map.entry("立方公尺", "m3"), Map.entry("m3", "m3"),
            Map.entry("米", "m"), Map.entry("延米", "m"), Map.entry("m", "m"),
            Map.entry("吨", "t"), Map.entry("公吨", "t"), Map.entry("t", "t"),
            Map.entry("千克", "kg"), Map.entry("公斤", "kg"), Map.entry("kg", "kg"),
            Map.entry("克", "g"), Map.entry("g", "g"),
            Map.entry("升", "l"), Map.entry("公升", "l"), Map.entry("l", "l"));

    public String normalizeCode(String value)
    {
        if (StringUtils.isBlank(value)) return "";
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", "");
    }

    public String normalizeText(String value)
    {
        if (StringUtils.isBlank(value)) return "";
        String source = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(source.length());
        source.codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint) || Character.isIdeographic(codePoint))
            {
                result.appendCodePoint(codePoint);
            }
            else if (codePoint == '#' || codePoint == '+' || codePoint == '-' || codePoint == '.'
                    || codePoint == '/' || codePoint == '%')
            {
                result.appendCodePoint(codePoint);
            }
            else if (codePoint == '×')
            {
                result.append('x');
            }
        });
        return result.toString();
    }

    public String normalizeUnit(String value)
    {
        String normalized = normalizeText(value);
        return UNIT_ALIASES.getOrDefault(normalized, normalized);
    }

    public double similarity(String left, String right)
    {
        if (left.equals(right)) return 1D;
        if (left.isEmpty() || right.isEmpty()) return 0D;
        return Math.max(levenshteinSimilarity(left, right), diceSimilarity(left, right));
    }

    private double levenshteinSimilarity(String left, String right)
    {
        if (left.length() > right.length()) return levenshteinSimilarity(right, left);
        int[] previous = new int[left.length() + 1];
        int[] current = new int[left.length() + 1];
        for (int i = 0; i <= left.length(); i++) previous[i] = i;
        for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++)
        {
            current[0] = rightIndex;
            for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++)
            {
                int cost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[leftIndex] = Math.min(Math.min(current[leftIndex - 1] + 1,
                        previous[leftIndex] + 1), previous[leftIndex - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return 1D - ((double) previous[left.length()] / Math.max(left.length(), right.length()));
    }

    private double diceSimilarity(String left, String right)
    {
        if (left.length() == 1 || right.length() == 1) return left.equals(right) ? 1D : 0D;
        java.util.Map<String, Integer> leftPairs = pairs(left);
        java.util.Map<String, Integer> rightPairs = pairs(right);
        int intersection = 0;
        for (Map.Entry<String, Integer> entry : leftPairs.entrySet())
        {
            intersection += Math.min(entry.getValue(), rightPairs.getOrDefault(entry.getKey(), 0));
        }
        return (2D * intersection) / ((left.length() - 1D) + (right.length() - 1D));
    }

    private java.util.Map<String, Integer> pairs(String value)
    {
        java.util.Map<String, Integer> pairs = new java.util.HashMap<>();
        for (int i = 0; i < value.length() - 1; i++)
        {
            pairs.merge(value.substring(i, i + 2), 1, Integer::sum);
        }
        return pairs;
    }
}
