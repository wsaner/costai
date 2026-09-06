package com.ruoyi.cost.knowledge.parse;

import java.util.regex.Pattern;

final class TextParsingSupport
{
    private static final Pattern HEADING = Pattern.compile("^(第[一二三四五六七八九十百零0-9]+[章节部分]|[一二三四五六七八九十]+、|[0-9]+(?:\\.[0-9]+){0,3}\\s+|附录\\s*[A-Z一二三四五六七八九十]).{0,80}$");

    private TextParsingSupport() { }

    static String normalize(String value)
    {
        return value == null ? "" : value.replace('\u0000', ' ').replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("[ ]{2,}", " ").trim();
    }

    static boolean isHeading(String text)
    {
        String value = normalize(text);
        return !value.isEmpty() && value.length() <= 100 && HEADING.matcher(value).matches();
    }
}
