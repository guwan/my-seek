package top.knos.mySeek.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.repository.KnowledgeDocumentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** 文件存储路径 */
    private final Path uploadDir;

    /** 单文件大小上限（字节） */
    private final long maxFileSize;

    public KnowledgeBaseService(KnowledgeDocumentRepository documentRepository,
                                DocumentParserService documentParserService,
                                VectorStoreService vectorStoreService,
                                @Value("${file.upload.path:uploads/documents/}") String uploadDir,
                                @Value("${file.upload.max-size:50MB}") String maxFileSize) {
        this.documentRepository = documentRepository;
        this.documentParserService = documentParserService;
        this.vectorStoreService = vectorStoreService;
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
     * 上传并处理文档。
     * <p>解析与向量化是耗时操作，这里不开启长事务，避免连接池被长时间占用。</p>
     */
    public KnowledgeDocument uploadDocument(MultipartFile file, Long userId) {
        // 1. 验证文件
        validateFile(file);

        // 2. 保存文件信息到数据库
        KnowledgeDocument document = new KnowledgeDocument();
        document.setName(generateUniqueFileName(file.getOriginalFilename()));
        document.setOriginalName(file.getOriginalFilename());
        document.setType(documentParserService.getFileExtension(file.getOriginalFilename()));
        document.setSize(file.getSize());
        document.setStatus(KnowledgeDocument.DocumentStatus.PENDING);
        document.setCreatedBy(userId);

        document = documentRepository.save(document);
        log.info("文档信息已保存: {}", document.getId());

        // 3. 异步处理文档（实际项目中应该使用异步任务）
        try {
            processDocument(document, file);
        } catch (Exception e) {
            log.error("文档处理失败: {}", document.getId(), e);
            document.setStatus(KnowledgeDocument.DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            throw new RuntimeException("文档处理失败: " + e.getMessage());
        }

        return document;
    }

    /**
     * 处理文档：解析内容并生成向量
     */
    public void processDocument(KnowledgeDocument document, MultipartFile file) {
        try {
            // 1. 更新状态为处理中
            document.setStatus(KnowledgeDocument.DocumentStatus.PROCESSING);
            documentRepository.save(document);

            // 2. 保存文件到本地
            String storagePath = saveFileToLocal(file, document.getName());
            document.setStoragePath(storagePath);

            // 3. 解析文档内容
            log.info("开始解析文档: {}", document.getId());
            String content = documentParserService.parseDocument(file);
            document.setContent(content);

            // 4. 生成向量并存储
            log.info("开始生成向量: {}", document.getId());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_name", document.getOriginalName());
            metadata.put("document_type", document.getType());
            metadata.put("upload_time", document.getCreatedAt().toString());

            int chunkCount = vectorStoreService.storeDocument(document.getId(), content, metadata);
            document.setChunkCount(chunkCount);

            // 5. 更新状态为完成
            document.setStatus(KnowledgeDocument.DocumentStatus.COMPLETED);
            documentRepository.save(document);

            log.info("文档处理完成: {}, 分块数: {}", document.getId(), chunkCount);

        } catch (Exception e) {
            log.error("文档处理失败: {}", document.getId(), e);
            document.setStatus(KnowledgeDocument.DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
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
    public Page<KnowledgeDocument> getDocumentsPage(int page, int size, KnowledgeDocument.DocumentStatus status) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return status == null
                ? documentRepository.findAllByOrderByCreatedAtDesc(pageable)
                : documentRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
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
    public List<KnowledgeDocument> getCompletedDocuments() {
        return documentRepository.findByStatusOrderByCreatedAtDesc(KnowledgeDocument.DocumentStatus.COMPLETED);
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(String documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

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
     * 基于知识库回答问题
     */
    public String answerBasedOnKnowledge(String query, int topK) {
        // 1. 搜索相关文档
        var relevantDocs = vectorStoreService.searchSimilarDocuments(query, topK);

        if (relevantDocs.isEmpty()) {
            return null; // 没有找到相关文档
        }

        // 2. 构建 RAG 提示词
        return vectorStoreService.buildRagPrompt(query, relevantDocs);
    }

    /**
     * 基于特定文档回答问题
     */
    public String answerBasedOnDocument(String documentId, String query, int topK) {
        // 1. 在特定文档中搜索
        var relevantDocs = vectorStoreService.searchInDocument(documentId, query, topK);

        if (relevantDocs.isEmpty()) {
            return null; // 没有找到相关内容
        }

        // 2. 构建 RAG 提示词
        return vectorStoreService.buildRagPrompt(query, relevantDocs);
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
