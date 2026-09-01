package com.ruoyi.web.controller.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.cost.file.dto.CostProjectFileCategoryRequest;
import jakarta.servlet.http.HttpServletResponse;

class CostProjectFileControllerPermissionTest
{
    @Test
    void everyProjectFileOperationHasItsOwnRbacPermission() throws Exception
    {
        assertPermission("list", "cost:file:list", Long.class);
        assertPermission("getInfo", "cost:file:query", Long.class);
        assertPermission("upload", "cost:file:upload", Long.class, String.class, MultipartFile.class);
        assertPermission("updateCategory", "cost:file:edit", Long.class, CostProjectFileCategoryRequest.class);
        assertPermission("remove", "cost:file:remove", Long.class);
        assertPermission("download", "cost:file:download", Long.class, HttpServletResponse.class);
        assertPermission("parseStatus", "cost:file:query", Long.class);
    }

    private void assertPermission(String methodName, String permission, Class<?>... parameterTypes) throws Exception
    {
        Method method = CostProjectFileController.class.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermi('" + permission + "')", annotation.value());
    }
}
