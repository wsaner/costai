package com.ruoyi.cost.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.mapper.CostProjectMapper;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import com.ruoyi.framework.security.context.PermissionContextHolder;

class CostProjectDataScopeAopTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void selfDataScopeIsInjectedIntoMapperQueryAtRuntime()
    {
        CostProjectMapper mapper = mock(CostProjectMapper.class);
        AtomicReference<CostProject> captured = new AtomicReference<>();
        when(mapper.selectCostProjectById(any(CostProject.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return new CostProject();
        });
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new CostProjectAccessService(mapper));
        proxyFactory.addAspect(new DataScopeAspect());
        CostProjectAccessService proxy = proxyFactory.getProxy();

        SysRole role = new SysRole(2L);
        role.setDataScope(Constants.Dept.DATA_SCOPE_SELF);
        role.setStatus("0");
        role.setPermissions(Set.of("cost:project:query"));
        SysUser user = new SysUser();
        user.setUserId(42L);
        user.setDeptId(100L);
        user.setUserName("scope_user");
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(42L, 100L, user, Set.of("cost:project:query"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        PermissionContextHolder.setContext("cost:project:query");

        CostProject query = new CostProject();
        query.setId(7L);
        proxy.selectAccessibleProject(query);

        assertNotNull(captured.get());
        String scopeSql = (String) captured.get().getParams().get(DataScopeAspect.DATA_SCOPE);
        assertTrue(scopeSql.contains("p.project_manager_id = 42"), scopeSql);
    }
}
