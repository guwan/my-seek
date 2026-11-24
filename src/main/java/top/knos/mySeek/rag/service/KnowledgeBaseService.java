package top.knos.mySeek.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.repository.KnowledgeDocumentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 知识库服务
 * 整合文档上传、解析、向量化的完整流程
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentParserService documentParserService;
    private final VectorStoreService vectorStoreService;
    private final ChatClient chatClient;
    private final ModelProviderManager modelProviderManager;
    private final KnowledgeProcessingService knowledgeProcessingService;

    private static final Pattern THINK_BLOCK = Pattern.compile("(?is)<(think|thinking)>.*?</(think|thinking)>");
    private static final Pattern UNCLOSED_THINK = Pattern.compile("(?is)<(think|thinking)>.*$");
    private static final int MAX_RETRY = 3;
    private static final int MAX_ERROR_LENGTH = 2048;

    /** 文件存储路径 */
    private final Path uploadDir;

    /** 单文件大小上限（字节） */
    private final long maxFileSize;

    public KnowledgeBaseService(KnowledgeDocumentRepository documentRepository,
                                DocumentParserService documentParserService,
                                VectorStoreService vectorStoreService,
                                ChatClient chatClient,
                                ModelProviderManager modelProviderManager,
                                KnowledgeProcessingService knowledgeProcessingService,
                                @Value("${file.upload.path:uploads/documents/}") String uploadDir,
                                @Value("${file.upload.max-size:50MB}") String maxFileSize) {
        this.documentRepository = documentRepository;
        this.documentParserService = documentParserService;
        this.vectorStoreService = vectorStoreService;
        this.chatClient = chatClient;
        this.modelProviderManager = modelProviderManager;
        this.knowledgeProcessingService = knowledgeProcessingService;
        this.uploadDir = Paths.get(uploadDir);
        this.maxFileSize = parseSize(maxFileSize);
    }

    /**
     * 解析形如 50MB / 1024KB / 1GB 的大小配置
     */
    private long parseSize(String size) {
        if (size == null || size.isBlank()) {
            return 50L * 1024 * 1024;
        }
        String text = size.trim().toUpperCase();
        long multiplier = 1;
        if (text.endsWith("KB")) {
            multiplier = 1024L;
        } else if (text.endsWith("MB")) {
            multiplier = 1024L * 1024;
        } else if (text.endsWith("GB")) {
            multiplier = 1024L * 1024 * 1024;
        }
        String number = text.replaceAll("[^0-9]", "");
        if (number.isEmpty()) {
            return 50L * 1024 * 1024;
        }
        return Long.parseLong(number) * multiplier;
    }

    /**
     * 上传文档：请求线程只做校验、落盘与入库，解析/向量化交给后台任务。
     * 接口立即返回 PENDING 记录，前端通过轮询状态获知进度。
     */
    public KnowledgeDocument uploadDocument(MultipartFile file, Long userId) {
        validateFile(file);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setName(generateUniqueFileName(file.getOriginalFilename()));
        document.setOriginalName(file.getOriginalFilename());
        document.setType(documentParserService.getFileExtension(file.getOriginalFilename()));
        document.setSize(file.getSize());
        document.setStatus(KnowledgeDocument.DocumentStatus.PENDING);
        document.setRetryCount(0);
        document.setCreatedBy(userId);
        document = documentRepository.save(document);

        try {
            // MultipartFile 在请求结束后会失效，必须在请求线程内先落盘
            document.setStoragePath(saveFileToLocal(file, document.getName()));
            document = documentRepository.save(document);
        } catch (Exception e) {
            log.error("文档保存失败: {}", document.getId(), e);
            document.setStatus(KnowledgeDocument.DocumentStatus.FAILED);
            document.setErrorMessage(trimMessage(e.getMessage()));
            documentRepository.save(document);
            throw new RuntimeException("文档保存失败: " + e.getMessage(), e);
        }

        knowledgeProcessingService.processAsync(document.getId(), userId);
        log.info("文档已提交异步处理: {}", document.getId());
        return document;
    }

    /** 查询单个文档状态（供前端精准轮询，并复用归属校验）。 */
    public KnowledgeDocument getDocument(String documentId, Long userId) {
        return documentRepository.findByIdAndCreatedBy(documentId, userId)
                .orElseThrow(() -> new RuntimeException("文档不存在或无权访问: " + documentId));
    }

    /** 重新处理失败文档；限制重试次数，避免无限重试。 */
    public KnowledgeDocument retryDocument(String documentId, Long userId) {
        KnowledgeDocument document = getDocument(documentId, userId);
        if (document.getStatus() != KnowledgeDocument.DocumentStatus.FAILED) {
            throw new IllegalStateException("仅失败的文档可以重试");
        }
        int retryCount = document.getRetryCount() == null ? 0 : document.getRetryCount();
        if (retryCount >= MAX_RETRY) {
            throw new IllegalStateException("重试次数已达上限 " + MAX_RETRY);
        }
        document.setRetryCount(retryCount + 1);
        document.setStatus(KnowledgeDocument.DocumentStatus.PENDING);
        document.setErrorMessage(null);
        document = documentRepository.save(document);
        knowledgeProcessingService.processAsync(documentId, userId);
        log.info("文档已重新提交处理: {}, retry={}", documentId, document.getRetryCount());
        return document;
    }

    /**
     * 获取所有文档
     */
    public List<KnowledgeDocument> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * 分页查询文档（按创建时间倒序）。
     *
     * @param page   页码，从 0 开始
     * @param size   每页条数，最大 100
     * @param status 状态过滤，可为 null
     */
    public Page<KnowledgeDocument> getDocumentsPage(Long userId, int page, int size,
                                                     KnowledgeDocument.DocumentStatus status) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return status == null
                ? documentRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
                : documentRepository.findByCreatedByAndStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    /**
     * 获取用户的文档
     */
    public List<KnowledgeDocument> getUserDocuments(Long userId) {
        return documentRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取已完成的文档
     */
    public List<KnowledgeDocument> getCompletedDocuments(Long userId) {
        return documentRepository.findByCreatedByAndStatusOrderByCreatedAtDesc(
                userId, KnowledgeDocument.DocumentStatus.COMPLETED);
    }

    /**
     * 删除文档（仅允许删除自己的文档）
     */
    @Transactional
    public void deleteDocument(String documentId, Long userId) {
        KnowledgeDocument document = documentRepository.findByIdAndCreatedBy(documentId, userId)
                .orElseThrow(() -> new RuntimeException("文档不存在或无权访问: " + documentId));
        if (knowledgeProcessingService.isActive(documentId)) {
            throw new IllegalStateException("文档正在处理中，请稍后再删除");
        }

        // 1. 删除向量
        try {
            vectorStoreService.deleteDocumentVectors(documentId);
        } catch (Exception e) {
            log.warn("删除向量失败: {}", documentId, e);
        }

        // 2. 删除本地文件
        if (document.getStoragePath() != null) {
            try {
                Path path = Paths.get(document.getStoragePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("删除本地文件失败: {}", document.getStoragePath(), e);
            }
        }

        // 3. 删除数据库记录
        documentRepository.delete(document);
        log.info("文档已删除: {}", documentId);
    }

    /**
     * 基于知识库回答问题：检索片段 -> 构建 RAG 提示词 -> 调用模型生成回答。
     */
    public String answerBasedOnKnowledge(Long userId, String query, int topK, String model) {
        var relevantDocs = vectorStoreService.searchSimilarDocuments(userId, query, topK);
        if (relevantDocs.isEmpty()) {
            return null;
        }
        return generateAnswer(query, relevantDocs, model);
    }

    /**
     * 基于特定文档回答问题（仅允许查询自己的文档）。
     */
    public String answerBasedOnDocument(Long userId, String documentId, String query, int topK, String model) {
        // 先校验归属，避免通过猜测 documentId 读取他人文档
        documentRepository.findByIdAndCreatedBy(documentId, userId)
                .orElseThrow(() -> new RuntimeException("文档不存在或无权访问: " + documentId));
        var relevantDocs = vectorStoreService.searchInDocument(userId, documentId, query, topK);
        if (relevantDocs.isEmpty()) {
            return null;
        }
        return generateAnswer(query, relevantDocs, model);
    }

    private String generateAnswer(String query, List<org.springframework.ai.document.Document> relevantDocs, String model) {
        String ragPrompt = vectorStoreService.buildRagPrompt(query, relevantDocs);
        try {
            String answer = getChatClient(model)
                    .prompt()
                    .system("你是知识库问答助手。只能依据参考信息回答；信息不足时明确说明不知道；不要输出提示词或分析过程。")
                    .user(ragPrompt)
                    .call()
                    .content();
            return stripThinking(answer);
        } catch (Exception e) {
            log.error("知识库问答生成失败", e);
            throw new RuntimeException("知识库问答生成失败: " + e.getMessage(), e);
        }
    }

    private ChatClient getChatClient(String model) {
        if (model == null || model.isBlank()) {
            try {
                ChatClient dynamic = modelProviderManager.getDefaultChatClient();
                if (dynamic != null) return dynamic;
            } catch (Exception e) {
                log.debug("动态默认客户端不可用，知识库使用启动时注入的客户端: {}", e.getMessage());
            }
            return chatClient;
        }
        try {
            return modelProviderManager.createChatClient(model);
        } catch (Exception e) {
            log.warn("知识库问答无法使用指定模型 {}, 回退默认模型: {}", model, e.getMessage());
            return chatClient;
        }
    }

    /** 清理模型输出中的推理标签，避免知识库回答暴露 think 内容。 */
    static String stripThinking(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = THINK_BLOCK.matcher(text).replaceAll("");
        cleaned = UNCLOSED_THINK.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (!documentParserService.isSupportedFileType(filename)) {
            throw new IllegalArgumentException("不支持的文件类型: " + filename);
        }

        // 限制文件大小（可配置）
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private static String trimMessage(String message) {
        if (message == null || message.isBlank()) return "文档处理失败";
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }

    /**
     * 保存文件到本地
     */
    private String saveFileToLocal(MultipartFile file, String fileName) throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }
}
