package com.ruoyi.web.controller.cost;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.cost.boq.dto.CostBoqImportRequest;

class CostBoqControllerPermissionTest
{
    @Test
    void importRequiresBothBoqAndFilePermission() throws Exception
    {
        Method method = CostBoqController.class.getMethod("importBoq", Long.class, CostBoqImportRequest.class);
        String expression = method.getAnnotation(PreAuthorize.class).value();
        assertTrue(expression.contains("cost:boq:import"));
        assertTrue(expression.contains("cost:file:query"));
        assertTrue(expression.contains(" and "));
    }
}
