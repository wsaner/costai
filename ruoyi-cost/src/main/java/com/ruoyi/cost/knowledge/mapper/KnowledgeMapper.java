package com.ruoyi.cost.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.knowledge.domain.KnowledgeBase;
import com.ruoyi.cost.knowledge.domain.KnowledgeChunk;
import com.ruoyi.cost.knowledge.domain.KnowledgeDocument;

public interface KnowledgeMapper
{
    List<KnowledgeBase> selectBaseList(KnowledgeBase query);
    KnowledgeBase selectBaseById(Long id);
    int countBaseName(@Param("name") String name, @Param("excludeId") Long excludeId);
    int insertBase(KnowledgeBase base);
    int updateBase(KnowledgeBase base);
    int updateBaseCollection(@Param("id") Long id, @Param("collection") String collection, @Param("operator") String operator);
    int deleteBase(@Param("id") Long id, @Param("operator") String operator);
    int refreshBaseCounts(Long id);
    int updateBaseEmbeddingModel(@Param("id") Long id, @Param("model") String model, @Param("operator") String operator);

    List<KnowledgeDocument> selectDocumentList(KnowledgeDocument query);
    KnowledgeDocument selectDocumentById(Long id);
    int countDocumentFile(@Param("baseId") Long baseId, @Param("projectFileId") Long projectFileId, @Param("excludeId") Long excludeId);
    int countByProjectFileId(Long projectFileId);
    int insertDocument(KnowledgeDocument document);
    int claimDocument(@Param("id") Long id, @Param("operator") String operator);
    int updateDocumentResult(KnowledgeDocument document);
    int resetDocument(@Param("id") Long id, @Param("operator") String operator);
    int failStaleDocuments(Long baseId);
    int deleteDocument(@Param("id") Long id, @Param("operator") String operator);
    int deleteDocumentsByBase(@Param("baseId") Long baseId, @Param("operator") String operator);

    List<KnowledgeChunk> selectChunkList(KnowledgeChunk query);
    List<KnowledgeChunk> selectChunksByDocument(Long documentId);
    List<KnowledgeChunk> selectChunksByIds(@Param("baseId") Long baseId, @Param("ids") List<Long> ids);
    int insertChunks(@Param("items") List<KnowledgeChunk> items);
    int markChunksSuccess(@Param("documentId") Long documentId, @Param("operator") String operator);
    int markChunksFailed(@Param("documentId") Long documentId, @Param("operator") String operator);
    int deleteChunksByDocument(@Param("documentId") Long documentId, @Param("operator") String operator);
    int deleteChunksByBase(@Param("baseId") Long baseId, @Param("operator") String operator);
}
