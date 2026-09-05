package com.ruoyi.web.controller.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.dto.BoqCompareRequest;
import com.ruoyi.cost.boq.match.dto.BoqManualMatchRequest;

class CostBoqCompareControllerPermissionTest
{
    @Test
    void everyEndpointUsesItsDedicatedRbacPermission() throws Exception
    {
        assertPermission("start", "cost:compare:start", BoqCompareRequest.class);
        assertPermission("rematch", "cost:compare:start", BoqCompareRequest.class);
        assertPermission("list", "cost:compare:list", CostBoqCompare.class);
        assertPermission("summary", "cost:compare:list", BoqCompareRequest.class);
        assertPermission("manual", "cost:compare:manual", BoqManualMatchRequest.class);
        assertPermission("unmatch", "cost:compare:manual", Long.class);
        assertPermission("batchOptions", "cost:compare:list", Long.class);
        assertPermission("itemOptions", "cost:compare:manual",
                Long.class, Long.class, String.class);
    }

    private void assertPermission(String methodName, String permission,
            Class<?>... parameterTypes) throws Exception
    {
        Method method = CostBoqCompareController.class.getMethod(methodName, parameterTypes);
        assertEquals("@ss.hasPermi('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
