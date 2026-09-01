package com.ruoyi.web.controller.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;

class CostProjectControllerContractTest
{
    @AfterEach
    void tearDown()
    {
        PageHelper.clearPage();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void crudEndpointsDeclareRequiredRbacPermissions() throws Exception
    {
        assertPermission("list", "cost:project:list", CostProject.class);
        assertPermission("getInfo", "cost:project:query", Long.class);
        assertPermission("add", "cost:project:add", CostProject.class);
        assertPermission("edit", "cost:project:edit", CostProject.class);
        assertPermission("remove", "cost:project:remove", Long[].class);
    }

    @Test
    void listReturnsFrameworkPaginationEnvelope()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ICostProjectService service = mock(ICostProjectService.class);
        CostProject row = new CostProject();
        row.setId(1L);
        when(service.selectCostProjectList(any(CostProject.class))).thenReturn(List.of(row));

        TableDataInfo result = new CostProjectController(service).list(new CostProject());

        assertEquals(200, result.getCode());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRows().size());
    }

    private void assertPermission(String methodName, String permission, Class<?> parameterType) throws Exception
    {
        Method method = CostProjectController.class.getMethod(methodName, parameterType);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + "必须声明权限");
        assertEquals("@ss.hasPermi('" + permission + "')", annotation.value());
    }
}
