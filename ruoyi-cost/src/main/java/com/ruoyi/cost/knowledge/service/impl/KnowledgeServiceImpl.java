package com.ruoyi.cost.knowledge.service.impl;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.ai.model.protocol.AiChatRequest;
import com.ruoyi.cost.ai.model.protocol.AiChatResponse;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingRequest;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingResponse;
import com.ruoyi.cost.ai.model.protocol.AiInvocationContext;
import com.ruoyi.cost.ai.model.protocol.AiMessage;
import com.ruoyi.cost.ai.model.protocol.AiTokenUsage;
import com.ruoyi.cost.ai.model.service.AiModelService;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.service.AiPromptTemplateService;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;
import com.ruoyi.cost.knowledge.chunk.KnowledgeChunkDraft;
import com.ruoyi.cost.knowledge.chunk.KnowledgeChunker;
import com.ruoyi.cost.knowledge.domain.KnowledgeBase;
import com.ruoyi.cost.knowledge.domain.KnowledgeChunk;
import com.ruoyi.cost.knowledge.domain.KnowledgeDocument;
import com.ruoyi.cost.knowledge.dto.KnowledgeBaseSaveRequest;
import com.ruoyi.cost.knowledge.dto.KnowledgeQueryRequest;
import com.ruoyi.cost.knowledge.mapper.KnowledgeMapper;
import com.ruoyi.cost.knowledge.parse.DocumentParserRegistry;
import com.ruoyi.cost.knowledge.parse.OcrRequiredException;
import com.ruoyi.cost.knowledge.parse.ParsedDocument;
import com.ruoyi.cost.knowledge.service.KnowledgeService;
import com.ruoyi.cost.knowledge.support.KnowledgeStatus;
import com.ruoyi.cost.knowledge.vector.VectorRecord;
import com.ruoyi.cost.knowledge.vector.VectorSearchResult;
import com.ruoyi.cost.knowledge.vector.VectorStoreService;
import com.ruoyi.cost.knowledge.vo.KnowledgeQueryVo;
import com.ruoyi.cost.knowledge.vo.KnowledgeSourceVo;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.github.pagehelper.PageHelper;

@Service
public class KnowledgeServiceImpl implements KnowledgeService
{
    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);
    private static final String RAG_PROMPT_CODE = "KNOWLEDGE_RAG_QA";
    private static final int EMBEDDING_BATCH = 32;
    private final KnowledgeMapper mapper;
    private final ICostProjectFileService fileService;
    private final ICostProjectService projectService;
    private final DocumentParserRegistry parserRegistry;
    private final KnowledgeChunker chunker;
    private final AiModelService aiModelService;
    private final AiPromptTemplateService promptService;
    private final VectorStoreService vectorStore;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public KnowledgeServiceImpl(KnowledgeMapper mapper, ICostProjectFileService fileService,
            ICostProjectService projectService, DocumentParserRegistry parserRegistry, KnowledgeChunker chunker,
            AiModelService aiModelService, AiPromptTemplateService promptService, VectorStoreService vectorStore,
            ObjectMapper objectMapper, @Qualifier("threadPoolTaskExecutor") Executor executor)
    {
        this.mapper = mapper; this.fileService = fileService; this.projectService = projectService;
        this.parserRegistry = parserRegistry; this.chunker = chunker; this.aiModelService = aiModelService;
        this.promptService = promptService; this.vectorStore = vectorStore; this.objectMapper = objectMapper;
        this.executor = executor;
    }

    @Override public List<KnowledgeBase> selectBaseList(KnowledgeBase query) { return mapper.selectBaseList(query); }

    @Override
    public KnowledgeBase selectBaseById(Long id)
    {
        KnowledgeBase base = id == null ? null : mapper.selectBaseById(id);
        if (base == null) throw new ServiceException("知识库不存在");
        return base;
    }

    @Override
    @Transactional
    public Long createBase(KnowledgeBaseSaveRequest request, String operator)
    {
        validateName(request.getName(), null);
        KnowledgeBase base = toBase(request, operator);
        if (mapper.insertBase(base) != 1) throw new ServiceException("知识库创建失败");
        String collection = "costai_kb_" + base.getId();
        mapper.updateBaseCollection(base.getId(), collection, operator);
        return base.getId();
    }

    @Override
    @Transactional
    public int updateBase(KnowledgeBaseSaveRequest request, String operator)
    {
        if (request.getId() == null) throw new ServiceException("知识库ID不能为空");
        selectBaseById(request.getId()); validateName(request.getName(), request.getId());
        KnowledgeBase base = toBase(request, operator); base.setId(request.getId());
        return mapper.updateBase(base);
    }

    @Override
    @Transactional
    public int deleteBase(Long id, String operator)
    {
        KnowledgeBase base = selectBaseById(id);
        List<KnowledgeDocument> documents = documents(id);
        for (KnowledgeDocument document : documents)
        {
            requireNotProcessing(document);
            fileService.selectProjectFileById(document.getProjectFileId());
        }
        mapper.deleteChunksByBase(id, operator); mapper.deleteDocumentsByBase(id, operator);
        int rows = mapper.deleteBase(id, operator);
        afterCommit(() -> safeDeleteCollection(base.getVectorCollection()));
        return rows;
    }

    @Override public List<KnowledgeDocument> selectDocuments(Long baseId) { selectBaseById(baseId); mapper.failStaleDocuments(baseId); return documents(baseId); }

    @Override
    public List<KnowledgeChunk> selectChunks(Long documentId)
    {
        KnowledgeDocument document = requireDocument(documentId);
        fileService.selectProjectFileById(document.getProjectFileId());
        PageDomain page = TableSupport.buildPageRequest();
        PageHelper.startPage(page.getPageNum(), page.getPageSize()).setReasonable(page.getReasonable());
        KnowledgeChunk query = new KnowledgeChunk(); query.setDocumentId(documentId);
        return mapper.selectChunkList(query);
    }

    @Override
    @Transactional
    public Long attachDocument(Long baseId, Long projectFileId, Long userId, String operator)
    {
        KnowledgeBase base = selectBaseById(baseId);
        if (!KnowledgeStatus.ENABLED.equals(base.getStatus())) throw new ServiceException("知识库已停用");
        CostProjectFile file = fileService.selectProjectFileById(projectFileId);
        String extension = normalizeExtension(file.getFileExt());
        parserRegistry.require(extension);
        if (mapper.countDocumentFile(baseId, projectFileId, null) > 0) throw new ServiceException("该文件已加入当前知识库");
        CostProjectFileDownloadVo download = fileService.prepareDownload(projectFileId);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(baseId); document.setProjectFileId(projectFileId);
        document.setDocumentName(file.getOriginalName()); document.setDocumentType(extension.toUpperCase(Locale.ROOT));
        document.setParseStatus(KnowledgeStatus.WAITING); document.setContentHash(file.getFileHash());
        document.setVectorCollection(base.getVectorCollection()); document.setCreateBy(operator);
        mapper.insertDocument(document); mapper.refreshBaseCounts(baseId);
        afterCommit(() -> executor.execute(() -> process(document.getId(), download.getPath(), extension, userId, operator)));
        return document.getId();
    }

    @Override
    @Transactional
    public int deleteDocument(Long id, String operator)
    {
        KnowledgeDocument document = requireDocument(id);
        requireNotProcessing(document);
        fileService.selectProjectFileById(document.getProjectFileId());
        mapper.deleteChunksByDocument(id, operator);
        int rows = mapper.deleteDocument(id, operator); mapper.refreshBaseCounts(document.getKnowledgeBaseId());
        afterCommit(() -> safeDeleteDocumentVectors(document.getVectorCollection(), id));
        return rows;
    }

    @Override
    @Transactional
    public int reindexDocument(Long id, Long userId, String operator)
    {
        KnowledgeDocument document = requireDocument(id);
        CostProjectFile file = fileService.selectProjectFileById(document.getProjectFileId());
        String extension = normalizeExtension(file.getFileExt()); parserRegistry.require(extension);
        CostProjectFileDownloadVo download = fileService.prepareDownload(file.getId());
        int rows = mapper.resetDocument(id, operator);
        if (rows == 0) throw new ServiceException("文档正在处理中或状态不允许重新索引");
        afterCommit(() -> executor.execute(() -> process(id, download.getPath(), extension, userId, operator)));
        return rows;
    }

    @Override public List<CostProject> projectOptions() { return projectService.selectCostProjectList(new CostProject()); }
    @Override public List<CostProjectFile> fileOptions(Long projectId) { return fileService.selectProjectFileList(projectId).stream().filter(file -> supports(file.getFileExt())).toList(); }

    @Override
    public KnowledgeQueryVo query(KnowledgeQueryRequest request, Long userId, String operator)
    {
        KnowledgeBase base = selectBaseById(request.getKnowledgeBaseId());
        if (!KnowledgeStatus.ENABLED.equals(base.getStatus())) throw new ServiceException("知识库已停用");
        int topK = request.getTopK() == null ? base.getTopK() : Math.max(1, Math.min(20, request.getTopK()));
        AiInvocationContext context = new AiInvocationContext(userId, operator, "KNOWLEDGE_RAG", String.valueOf(base.getId()));
        AiEmbeddingResponse embedded = embed(List.of(request.getQuestion().trim()), context);
        List<VectorSearchResult> hits = vectorStore.searchSimilar(base.getVectorCollection(), embedded.embeddings().get(0), topK, base.getSimilarityThreshold());
        if (hits.isEmpty()) return new KnowledgeQueryVo("未在该知识库中检索到足够相关的依据。", List.of(), embedded.tokenUsage());
        List<Long> ids = hits.stream().map(hit -> parseId(hit.id())).filter(id -> id != null).toList();
        if (ids.isEmpty()) return new KnowledgeQueryVo("未在该知识库中检索到有效依据。", List.of(), embedded.tokenUsage());
        Map<Long, KnowledgeChunk> chunks = mapper.selectChunksByIds(base.getId(), ids).stream().collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity()));
        StringBuilder retrieved = new StringBuilder(); List<KnowledgeSourceVo> sources = new ArrayList<>(); int sourceNo = 1;
        for (VectorSearchResult hit : hits)
        {
            KnowledgeChunk chunk = chunks.get(parseId(hit.id())); if (chunk == null) continue;
            String citation = "[来源" + sourceNo++ + "]《" + chunk.getDocumentName() + "》" + (chunk.getPageNumber() == null ? "" : " 第" + chunk.getPageNumber() + "页") + (StringUtils.isBlank(chunk.getSectionTitle()) ? "" : " " + chunk.getSectionTitle());
            String addition = citation + "\n" + chunk.getContent() + "\n\n";
            if (retrieved.length() + addition.length() > base.getMaxContextChars()) break;
            retrieved.append(addition);
            sources.add(new KnowledgeSourceVo(chunk.getId(), chunk.getDocumentName(), chunk.getPageNumber(), chunk.getSectionTitle(), hit.score(), quote(chunk.getContent())));
        }
        if (sources.isEmpty()) return new KnowledgeQueryVo("检索结果超过上下文限制，未生成回答。", List.of(), embedded.tokenUsage());
        AiPromptTemplate prompt = promptService.selectActive(RAG_PROMPT_CODE);
        if (prompt == null) throw new ServiceException("知识库问答Prompt未配置或未启用");
        String userPrompt = prompt.getUserTemplate().replace("{{question}}", request.getQuestion()).replace("{{context}}", retrieved.toString());
        AiChatRequest chat = new AiChatRequest(); chat.setMessages(List.of(new AiMessage("system", prompt.getSystemPrompt()), new AiMessage("user", userPrompt)));
        chat.setMaxTokens(1600); chat.setContext(context);
        AiChatResponse response = aiModelService.chat(chat);
        return new KnowledgeQueryVo(response.content(), sources, sum(embedded.tokenUsage(), response.tokenUsage()));
    }

    void process(Long documentId, Path path, String extension, Long userId, String operator)
    {
        KnowledgeDocument document = mapper.selectDocumentById(documentId); if (document == null || mapper.claimDocument(documentId, operator) != 1) return;
        try
        {
            mapper.deleteChunksByDocument(documentId, operator);
            safeDeleteDocumentVectors(document.getVectorCollection(), documentId);
            ParsedDocument parsed = parserRegistry.require(extension).parse(path);
            List<KnowledgeChunkDraft> drafts = chunker.chunk(parsed.blocks());
            if (drafts.isEmpty()) throw new ServiceException("文档没有可索引的有效内容");
            KnowledgeBase base = selectBaseById(document.getKnowledgeBaseId());
            List<KnowledgeChunk> entities = drafts.stream().map(draft -> toChunk(base, document, draft, operator)).toList();
            for (int from = 0; from < entities.size(); from += 500)
                mapper.insertChunks(entities.subList(from, Math.min(entities.size(), from + 500)));
            entities = mapper.selectChunksByDocument(documentId);
            AiInvocationContext context = new AiInvocationContext(userId, operator, "KNOWLEDGE_EMBEDDING", String.valueOf(documentId));
            List<VectorRecord> records = new ArrayList<>(); String model = null; int dimension = -1;
            for (int from = 0; from < entities.size(); from += EMBEDDING_BATCH)
            {
                List<KnowledgeChunk> batch = entities.subList(from, Math.min(entities.size(), from + EMBEDDING_BATCH));
                AiEmbeddingResponse response = embed(batch.stream().map(KnowledgeChunk::getContent).toList(), context);
                if (response.embeddings().size() != batch.size()) throw new ServiceException("Embedding返回数量不一致");
                model = response.model();
                for (int i = 0; i < batch.size(); i++)
                {
                    KnowledgeChunk chunk = batch.get(i); List<Double> vector = response.embeddings().get(i);
                    if (dimension < 0) dimension = vector.size();
                    records.add(new VectorRecord(chunk.getId(), vector, Map.of("knowledgeBaseId", base.getId(), "documentId", documentId, "chunkId", chunk.getId())));
                }
            }
            vectorStore.saveVectors(base.getVectorCollection(), dimension, records);
            mapper.markChunksSuccess(documentId, operator);
            mapper.updateBaseEmbeddingModel(base.getId(), model, operator);
            updateResult(document, KnowledgeStatus.SUCCESS, entities, model, null, operator);
        }
        catch (OcrRequiredException e) { updateResult(document, KnowledgeStatus.OCR_REQUIRED, List.of(), null, e.getMessage(), operator); }
        catch (Exception e)
        {
            mapper.markChunksFailed(documentId, operator); safeDeleteDocumentVectors(document.getVectorCollection(), documentId);
            updateResult(document, KnowledgeStatus.FAILED, mapper.selectChunksByDocument(documentId), null, safeMessage(e), operator);
            log.warn("知识文档索引失败，documentId={}: {}", documentId, safeMessage(e));
        }
    }

    private void updateResult(KnowledgeDocument document, String status, List<KnowledgeChunk> chunks, String model, String error, String operator)
    {
        document.setParseStatus(status); document.setChunkCount(KnowledgeStatus.SUCCESS.equals(status) ? chunks.size() : 0);
        document.setCharCount(chunks.stream().mapToInt(item -> item.getContent() == null ? 0 : item.getContent().length()).sum());
        document.setEmbeddingModel(model); document.setErrorMessage(error); document.setIndexedTime(KnowledgeStatus.SUCCESS.equals(status) ? new Date() : null); document.setUpdateBy(operator);
        mapper.updateDocumentResult(document); mapper.refreshBaseCounts(document.getKnowledgeBaseId());
    }

    private KnowledgeChunk toChunk(KnowledgeBase base, KnowledgeDocument document, KnowledgeChunkDraft draft, String operator)
    {
        KnowledgeChunk chunk = new KnowledgeChunk(); chunk.setKnowledgeBaseId(base.getId()); chunk.setDocumentId(document.getId());
        chunk.setContent(draft.content()); chunk.setPageNumber(draft.pageNumber()); chunk.setSectionTitle(draft.sectionTitle());
        chunk.setChunkIndex(draft.chunkIndex()); chunk.setCharCount(draft.content().length()); chunk.setContentHash(sha256(draft.content()));
        try { chunk.setMetadataJson(objectMapper.writeValueAsString(Map.of("documentName", document.getDocumentName(), "pageNumber", draft.pageNumber() == null ? 0 : draft.pageNumber(), "sectionTitle", draft.sectionTitle() == null ? "" : draft.sectionTitle()))); }
        catch (Exception e) { throw new ServiceException("分片元数据生成失败"); }
        chunk.setVectorStore("QDRANT"); chunk.setVectorCollection(base.getVectorCollection()); chunk.setCreateBy(operator); return chunk;
    }

    private AiEmbeddingResponse embed(List<String> inputs, AiInvocationContext context)
    {
        AiEmbeddingRequest request = new AiEmbeddingRequest(); request.setInputs(inputs); request.setContext(context);
        AiEmbeddingResponse response = aiModelService.embedding(request);
        if (response.embeddings() == null || response.embeddings().isEmpty()) throw new ServiceException("Embedding未返回向量");
        return response;
    }

    private KnowledgeBase toBase(KnowledgeBaseSaveRequest request, String operator)
    {
        KnowledgeBase base = new KnowledgeBase(); base.setName(request.getName().trim()); base.setDescription(StringUtils.trim(request.getDescription()));
        base.setStatus(KnowledgeStatus.DISABLED.equals(request.getStatus()) ? KnowledgeStatus.DISABLED : KnowledgeStatus.ENABLED);
        base.setTopK(request.getTopK() == null ? 5 : request.getTopK()); base.setSimilarityThreshold(request.getSimilarityThreshold() == null ? new BigDecimal("0.55") : request.getSimilarityThreshold());
        base.setMaxContextChars(request.getMaxContextChars() == null ? 12000 : request.getMaxContextChars()); base.setCreateBy(operator); base.setUpdateBy(operator); return base;
    }

    private void validateName(String name, Long excludeId)
    {
        String value = StringUtils.trim(name); if (StringUtils.isBlank(value)) throw new ServiceException("知识库名称不能为空");
        if (mapper.countBaseName(value, excludeId) > 0) throw new ServiceException("知识库名称已存在");
    }
    private List<KnowledgeDocument> documents(Long baseId) { KnowledgeDocument query = new KnowledgeDocument(); query.setKnowledgeBaseId(baseId); return mapper.selectDocumentList(query); }
    private KnowledgeDocument requireDocument(Long id) { KnowledgeDocument value = id == null ? null : mapper.selectDocumentById(id); if (value == null) throw new ServiceException("知识文档不存在"); return value; }
    private void requireNotProcessing(KnowledgeDocument document) { if (KnowledgeStatus.WAITING.equals(document.getParseStatus()) || KnowledgeStatus.PARSING.equals(document.getParseStatus())) throw new ServiceException("文档正在解析或索引，请处理完成后再删除"); }
    private boolean supports(String extension) { try { parserRegistry.require(normalizeExtension(extension)); return true; } catch (ServiceException e) { return false; } }
    private String normalizeExtension(String extension) { return FilenameUtils.getExtension("x." + StringUtils.trim(extension)).toLowerCase(Locale.ROOT); }
    private Long parseId(String id) { try { return Long.valueOf(id); } catch (Exception e) { return null; } }
    private String quote(String text) { String value = text.replaceAll("\\s+", " ").trim(); return value.length() <= 300 ? value : value.substring(0, 300) + "…"; }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private AiTokenUsage sum(AiTokenUsage a, AiTokenUsage b) { a = a == null ? AiTokenUsage.EMPTY : a; b = b == null ? AiTokenUsage.EMPTY : b; return new AiTokenUsage(a.promptTokens()+b.promptTokens(), a.completionTokens()+b.completionTokens(), a.totalTokens()+b.totalTokens()); }
    private void afterCommit(Runnable action) { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { action.run(); } }); else action.run(); }
    private void safeDeleteCollection(String collection) { try { vectorStore.deleteCollection(collection); } catch (Exception e) { log.warn("删除知识库向量集合失败: {}", safeMessage(e)); } }
    private void safeDeleteDocumentVectors(String collection, Long documentId) { try { if (StringUtils.isNotBlank(collection)) vectorStore.deleteDocumentVectors(collection, documentId); } catch (Exception e) { log.warn("清理文档向量失败，documentId={}: {}", documentId, safeMessage(e)); } }
    private String safeMessage(Exception e) { String value = e.getMessage(); if (StringUtils.isBlank(value)) value = e.getClass().getSimpleName(); return value.length() > 500 ? value.substring(0,500) : value; }
}
