package top.knos.mySeek.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.repository.KnowledgeDocumentRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 知识库文档的耗时处理：解析 + 向量化。
 * 由独立 Bean 承载 @Async，避免同类自调用导致代理失效。
 */
@Slf4j
@Service
public class KnowledgeProcessingService {

    private static final int MAX_ERROR_LENGTH = 2048;

    /** 正在后台处理的文档，删除操作需要避开这些记录，防止任务结束后 resurrect 已删除记录 */
    private final Set<String> activeDocuments = ConcurrentHashMap.newKeySet();

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentParserService documentParserService;
    private final VectorStoreService vectorStoreService;
    private final Executor taskExecutor;

    public KnowledgeProcessingService(KnowledgeDocumentRepository documentRepository,
                                      DocumentParserService documentParserService,
                                      VectorStoreService vectorStoreService,
                                      @Qualifier("knowledgeTaskExecutor") Executor taskExecutor) {
        this.documentRepository = documentRepository;
        this.documentParserService = documentParserService;
        this.vectorStoreService = vectorStoreService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 在后台线程解析文档并生成向量。失败时只落库，不把异常抛回线程池。
     */
    @Async("knowledgeTaskExecutor")
    public void processAsync(String documentId, Long userId) {
        var found = documentRepository.findByIdAndCreatedBy(documentId, userId);
        if (found.isEmpty()) {
            log.warn("异步任务找不到文档或用户无权访问: documentId={}, userId={}", documentId, userId);
            return;
        }
        KnowledgeDocument document = found.get();
        if (document.getStatus() == KnowledgeDocument.DocumentStatus.COMPLETED) {
            return;
        }
        if (!activeDocuments.add(documentId)) {
            log.warn("文档已有任务在处理中，忽略重复投递: {}", documentId);
            return;
        }

        try {
            document.setStatus(KnowledgeDocument.DocumentStatus.PROCESSING);
            document.setErrorMessage(null);
            document.setLastAttemptAt(LocalDateTime.now());
            documentRepository.save(document);

            // 重试前清理旧向量，避免同一 document_id 产生重复分块
            try {
                vectorStoreService.deleteDocumentVectors(documentId);
            } catch (Exception e) {
                log.debug("清理旧向量跳过: documentId={}, reason={}", documentId, e.getMessage());
            }

            if (document.getStoragePath() == null || document.getStoragePath().isBlank()) {
                throw new IllegalStateException("文档文件不存在，请重新上传");
            }
            Path path = Paths.get(document.getStoragePath());
            if (!Files.exists(path)) {
                throw new IllegalStateException("文档文件已丢失，请重新上传");
            }

            String content = documentParserService.parseDocument(path);
            document.setContent(content);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_name", document.getOriginalName());
            metadata.put("document_type", document.getType());
            metadata.put("upload_time", document.getCreatedAt() == null
                    ? LocalDateTime.now().toString() : document.getCreatedAt().toString());
            if (document.getCreatedBy() != null) {
                metadata.put("created_by", String.valueOf(document.getCreatedBy()));
            }

            int chunkCount = vectorStoreService.storeDocument(documentId, content, metadata);
            document.setChunkCount(chunkCount);
            document.setStatus(KnowledgeDocument.DocumentStatus.COMPLETED);
            document.setErrorMessage(null);
            documentRepository.save(document);

            log.info("文档异步处理完成: documentId={}, userId={}, chunks={}", documentId, userId, chunkCount);
        } catch (Exception e) {
            log.error("文档异步处理失败: documentId={}, userId={}", documentId, userId, e);
            document.setStatus(KnowledgeDocument.DocumentStatus.FAILED);
            document.setErrorMessage(trimMessage(e.getMessage()));
            documentRepository.save(document);
        } finally {
            activeDocuments.remove(documentId);
        }
    }

    /** 文档是否正在后台处理；删除操作需要避开这些记录。 */
    public boolean isActive(String documentId) {
        return activeDocuments.contains(documentId);
    }

    /**
     * 服务重启后把未进入终态的任务重新投递，避免 PENDING/PROCESSING 永久卡住。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeUnfinishedDocuments() {
        // 恢复是增强功能，历史库缺少知识库表时不能阻止整个应用启动。
        try {
            List<KnowledgeDocument> unfinished = documentRepository.findByStatusIn(
                    List.of(KnowledgeDocument.DocumentStatus.PENDING, KnowledgeDocument.DocumentStatus.PROCESSING));
            for (KnowledgeDocument document : unfinished) {
                try {
                    if (document.getId() == null) continue;
                    document.setStatus(KnowledgeDocument.DocumentStatus.PENDING);
                    document.setErrorMessage(null);
                    documentRepository.save(document);
                    final String id = document.getId();
                    final Long owner = document.getCreatedBy();
                    taskExecutor.execute(() -> processAsync(id, owner));
                    log.info("已重新投递未完成的知识库任务: {}", id);
                } catch (Exception e) {
                    log.warn("恢复单个知识库任务失败，跳过: id={}, reason={}", document.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("扫描未完成知识库任务失败，已跳过恢复；请确认 knowledge_documents 表存在", e);
        }
    }

    private static String trimMessage(String message) {
        if (message == null || message.isBlank()) return "文档处理失败";
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
