package com.ruoyi.cost.file.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.ruoyi.common.exception.ServiceException;

class CostProjectFileValidatorTest
{
    private final CostProjectFileValidator validator = new CostProjectFileValidator();

    @Test
    void acceptsSupportedTextFileAndNormalizesExtension()
    {
        MockMultipartFile file = new MockMultipartFile("file", "清单.TXT", "text/plain", "内容".getBytes());
        assertEquals("txt", validator.validate(file));
    }

    @Test
    void rejectsEmptyAndUnsupportedFiles()
    {
        assertThrows(ServiceException.class, () -> validator.validate(
                new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])));
        assertThrows(ServiceException.class, () -> validator.validate(
                new MockMultipartFile("file", "script.exe", "application/octet-stream", new byte[] { 1 })));
    }

    @Test
    void rejectsRenamedPdfBySignature()
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> validator.validate(
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "not-pdf".getBytes())));
        assertEquals("文件内容与扩展名不匹配", exception.getMessage());
    }

    @Test
    void rejectsBinaryContentRenamedAsText()
    {
        assertThrows(ServiceException.class, () -> validator.validate(
                new MockMultipartFile("file", "fake.txt", "text/plain", new byte[] { 1, 0, 2 })));
    }

    @Test
    void storageOnlyCadFormatsRemainUploadable()
    {
        MockMultipartFile file = new MockMultipartFile("file", "drawing.dwg",
                "application/octet-stream", "AC1032".getBytes());
        assertEquals("dwg", validator.validate(file));
        assertEquals(CostProjectFileParseStatus.UNSUPPORTED,
                CostProjectFileParseStatus.initialStatus("dwg"));
    }

    @Test
    void acceptsOleContainerForPasswordProtectedXlsxSoParserCanGiveFriendlyError()
    {
        byte[] oleHeader = { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 0, 0, 0 };
        MockMultipartFile file = new MockMultipartFile("file", "protected.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", oleHeader);
        assertEquals("xlsx", validator.validate(file));
    }
}
