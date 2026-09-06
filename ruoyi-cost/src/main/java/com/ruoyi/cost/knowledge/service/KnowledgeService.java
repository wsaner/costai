package com.ruoyi.cost.knowledge.service;

import java.util.List;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.knowledge.domain.KnowledgeBase;
import com.ruoyi.cost.knowledge.domain.KnowledgeChunk;
import com.ruoyi.cost.knowledge.domain.KnowledgeDocument;
import com.ruoyi.cost.knowledge.dto.KnowledgeBaseSaveRequest;
import com.ruoyi.cost.knowledge.dto.KnowledgeQueryRequest;
import com.ruoyi.cost.knowledge.vo.KnowledgeQueryVo;
import com.ruoyi.cost.project.domain.CostProject;

public interface KnowledgeService
{
    List<KnowledgeBase> selectBaseList(KnowledgeBase query);
    KnowledgeBase selectBaseById(Long id);
    Long createBase(KnowledgeBaseSaveRequest request, String operator);
    int updateBase(KnowledgeBaseSaveRequest request, String operator);
    int deleteBase(Long id, String operator);
    List<KnowledgeDocument> selectDocuments(Long baseId);
    List<KnowledgeChunk> selectChunks(Long documentId);
    Long attachDocument(Long baseId, Long projectFileId, Long userId, String operator);
    int deleteDocument(Long id, String operator);
    int reindexDocument(Long id, Long userId, String operator);
    List<CostProject> projectOptions();
    List<CostProjectFile> fileOptions(Long projectId);
    KnowledgeQueryVo query(KnowledgeQueryRequest request, Long userId, String operator);
}
