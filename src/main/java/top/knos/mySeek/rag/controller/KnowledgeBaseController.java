package top.knos.mySeek.rag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.service.KnowledgeBaseService;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.support.SecurityUtils;

import java.util.List;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Result<KnowledgeDocument> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("上传文档: {}, 用户: {}", file.getOriginalFilename(), currentUserId());
        return Result.ok(knowledgeBaseService.uploadDocument(file, currentUserId()));
    }

    /**
     * 分页获取文档列表
     */
    @GetMapping("/documents")
    public Result<Page<KnowledgeDocument>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) KnowledgeDocument.DocumentStatus status) {
        return Result.ok(knowledgeBaseService.getDocumentsPage(page, size, status));
    }

    /**
     * 获取已完成的文档
     */
    @GetMapping("/documents/completed")
    public Result<List<KnowledgeDocument>> getCompletedDocuments() {
        return Result.ok(knowledgeBaseService.getCompletedDocuments());
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{documentId}")
    public Result<Object> deleteDocument(@PathVariable String documentId) {
        knowledgeBaseService.deleteDocument(documentId);
        return Result.ok(null);
    }

    /**
     * 基于知识库回答问题（RAG）
     */
    @PostMapping("/query")
    public Result<String> queryKnowledgeBase(@RequestBody QueryRequest request) {
        String answer = knowledgeBaseService.answerBasedOnKnowledge(
                request.getQuery(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getModel()
        );
        return Result.ok(answer == null ? "没有找到相关文档内容" : answer);
    }

    /**
     * 基于特定文档回答问题
     */
    @PostMapping("/documents/{documentId}/query")
    public Result<String> queryDocument(@PathVariable String documentId, @RequestBody QueryRequest request) {
        String answer = knowledgeBaseService.answerBasedOnDocument(
                documentId,
                request.getQuery(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getModel()
        );
        return Result.ok(answer == null ? "在该文档中没有找到相关内容" : answer);
    }

    /**
     * 获取当前登录用户 ID，未登录时返回 null。
     */
    private Long currentUserId() {
        User user = SecurityUtils.getUser();
        return user == null ? null : user.getId();
    }

    // 请求体
    public static class QueryRequest {
        private String query;
        private Integer topK;
        private String model;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
