package com.ruoyi.cost.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.cost.project.domain.CostProject;

class CostProjectDataScopeContractTest
{
    @Test
    void listDetailAndStatisticsUseExistingDepartmentAndSelfScopeFields() throws Exception
    {
        assertDataScope("selectCostProjectList", CostProject.class);
        assertDataScope("selectProjectStatistics", CostProject.class);
        Method detailMethod = CostProjectAccessService.class.getMethod("selectAccessibleProject", CostProject.class);
        assertScope(detailMethod, "selectAccessibleProject");
    }

    private void assertDataScope(String methodName, Class<?> parameterType) throws Exception
    {
        Method method = CostProjectServiceImpl.class.getMethod(methodName, parameterType);
        assertScope(method, methodName);
    }

    private void assertScope(Method method, String methodName)
    {
        DataScope scope = method.getAnnotation(DataScope.class);
        assertNotNull(scope, methodName + "必须接入现有数据权限");
        assertEquals("p", scope.deptAlias());
        assertEquals("owner_dept_id", scope.deptField());
        assertEquals("p", scope.userAlias());
        assertEquals("project_manager_id", scope.userField());
    }
}
