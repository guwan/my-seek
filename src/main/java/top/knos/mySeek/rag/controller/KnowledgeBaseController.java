package top.knos.mySeek.rag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import top.knos.mySeek.exception.CustomHttpException;
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
        return Result.ok(knowledgeBaseService.getDocumentsPage(currentUserId(), page, size, status));
    }

    /**
     * 获取已完成的文档
     */
    @GetMapping("/documents/completed")
    public Result<List<KnowledgeDocument>> getCompletedDocuments() {
        return Result.ok(knowledgeBaseService.getCompletedDocuments(currentUserId()));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{documentId}")
    public Result<Object> deleteDocument(@PathVariable String documentId) {
        knowledgeBaseService.deleteDocument(documentId, currentUserId());
        return Result.ok(null);
    }

    /**
     * 查询单个文档的当前状态（供前端轮询处理进度）
     */
    @GetMapping("/documents/{documentId}")
    public Result<KnowledgeDocument> getDocument(@PathVariable String documentId) {
        return Result.ok(knowledgeBaseService.getDocument(documentId, currentUserId()));
    }

    /**
     * 重新处理失败的文档
     */
    @PostMapping("/documents/{documentId}/retry")
    public Result<KnowledgeDocument> retryDocument(@PathVariable String documentId) {
        return Result.ok(knowledgeBaseService.retryDocument(documentId, currentUserId()));
    }

    /**
     * 基于知识库回答问题（RAG）
     */
    @PostMapping("/query")
    public Result<String> queryKnowledgeBase(@RequestBody QueryRequest request) {
        String answer = knowledgeBaseService.answerBasedOnKnowledge(
                currentUserId(),
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
                currentUserId(),
                documentId,
                request.getQuery(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getModel()
        );
        return Result.ok(answer == null ? "在该文档中没有找到相关内容" : answer);
    }

    /** 获取当前登录用户 ID；未登录直接拒绝，避免出现无归属文档或跨用户数据。 */
    private Long currentUserId() {
        User user = SecurityUtils.getUser();
        if (user == null || user.getId() == null) {
            throw new CustomHttpException("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
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
