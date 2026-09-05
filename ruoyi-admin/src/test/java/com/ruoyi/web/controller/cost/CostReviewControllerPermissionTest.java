package com.ruoyi.web.controller.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;
import com.ruoyi.cost.review.dto.ReviewStartRequest;
import com.ruoyi.cost.review.dto.RuleConfigUpdateRequest;
import com.ruoyi.cost.review.dto.CostReviewAiRequest;

class CostReviewControllerPermissionTest
{
    @Test
    void everyReviewEndpointUsesExistingRbacConvention() throws Exception
    {
        assertPermission("start", "cost:review:start", ReviewStartRequest.class);
        assertPermission("list", "cost:review:list", CostReviewTask.class);
        assertPermission("info", "cost:review:query", Long.class);
        assertPermission("issues", "cost:review:list", Long.class, CostReviewIssue.class);
        assertPermission("issueInfo", "cost:review:query", Long.class);
        assertPermission("handleIssue", "cost:review:handle", Long.class,
                ReviewIssueHandleRequest.class);
        assertPermission("analyzeIssue", "cost:review:ai", Long.class, CostReviewAiRequest.class);
        assertPermission("ruleConfigs", "cost:review:config");
        assertPermission("updateRuleConfig", "cost:review:config", Long.class,
                RuleConfigUpdateRequest.class);
    }

    private void assertPermission(String methodName, String permission,
            Class<?>... parameterTypes) throws Exception
    {
        Method method = CostReviewController.class.getMethod(methodName, parameterTypes);
        assertEquals("@ss.hasPermi('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
