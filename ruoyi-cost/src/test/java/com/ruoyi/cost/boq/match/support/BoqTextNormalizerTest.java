package com.ruoyi.cost.boq.match.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class BoqTextNormalizerTest
{
    private final BoqTextNormalizer normalizer = new BoqTextNormalizer();

    @Test
    void normalizesWidthWhitespaceBracketsAndCaseWithoutDroppingModelSymbols()
    {
        assertEquals("AB-001", normalizer.normalizeCode(" ＡＢ - ００１ "));
        assertEquals("c30混凝土dn-100#", normalizer.normalizeText(" C30混凝土（DN-100#） "));
        assertEquals("axb", normalizer.normalizeText("A×B"));
    }

    @Test
    void normalizesCommonEngineeringUnits()
    {
        assertEquals("m2", normalizer.normalizeUnit("㎡"));
        assertEquals("m2", normalizer.normalizeUnit("平方米"));
        assertEquals("m3", normalizer.normalizeUnit("m³"));
        assertEquals("kg", normalizer.normalizeUnit("公斤"));
    }

    @Test
    void similarityRecognizesSmallTyposAndRejectsUnrelatedText()
    {
        assertTrue(normalizer.similarity("现浇混凝土", "现浇混泥土") > 0.75D);
        assertTrue(normalizer.similarity("现浇混凝土", "电力电缆") < 0.4D);
    }
}
