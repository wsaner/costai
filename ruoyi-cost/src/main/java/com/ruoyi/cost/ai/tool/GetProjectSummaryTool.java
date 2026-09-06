package com.ruoyi.cost.ai.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.ai.chat.mapper.AiChatMapper;
import com.ruoyi.cost.project.domain.CostProject;

@Component
public class GetProjectSummaryTool implements AiTool<ProjectToolInput, Map<String, Object>>
{
    private final AiChatMapper mapper;
    public GetProjectSummaryTool(AiChatMapper mapper) { this.mapper = mapper; }
    @Override public String getName() { return "getProjectSummary"; }
    @Override public Map<String, Object> execute(ProjectToolInput input)
    {
        CostProject project = input.project();
        if (project == null || !project.getId().equals(input.projectId()))
            throw new IllegalArgumentException("工具调用缺少已授权的项目上下文");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", project.getId()); result.put("projectCode", project.getProjectCode());
        result.put("projectName", project.getProjectName()); result.put("projectType", project.getProjectType());
        result.put("professionalType", project.getProfessionalType()); result.put("projectStage", project.getProjectStage());
        result.put("region", String.join("/", safe(project.getProvince()), safe(project.getCity()), safe(project.getDistrict())));
        result.put("buildingArea", project.getBuildingArea()); result.put("projectManager", project.getProjectManagerName());
        result.put("submittedAmount", project.getSubmittedAmount()); result.put("approvedAmount", project.getApprovedAmount());
        result.put("reductionAmount", project.getReductionAmount()); result.put("reductionRate", project.getReductionRate());
        result.put("projectStatus", project.getProjectStatus());
        result.put("boq", mapper.selectBoqSummary(input.projectId()));
        result.put("latestReview", mapper.selectReviewSummary(input.projectId()));
        return result;
    }
    private String safe(String value) { return value == null ? "" : value; }
}
