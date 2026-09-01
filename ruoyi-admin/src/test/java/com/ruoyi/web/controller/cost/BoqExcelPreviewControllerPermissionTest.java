package com.ruoyi.web.controller.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.cost.boq.preview.dto.BoqPreviewRequest;

class BoqExcelPreviewControllerPermissionTest
{
    @Test
    void uploadAndRepreviewRequireBoqAndFilePermissions() throws Exception
    {
        Method upload = BoqExcelPreviewController.class.getMethod("upload", Long.class, MultipartFile.class);
        assertEquals("@ss.hasPermi('cost:boq:preview') and @ss.hasPermi('cost:file:upload')",
                upload.getAnnotation(PreAuthorize.class).value());

        Method preview = BoqExcelPreviewController.class.getMethod("preview", Long.class, BoqPreviewRequest.class);
        assertEquals("@ss.hasPermi('cost:boq:preview') and @ss.hasPermi('cost:file:query')",
                preview.getAnnotation(PreAuthorize.class).value());
    }
}
