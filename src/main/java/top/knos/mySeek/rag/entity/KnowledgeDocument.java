package top.knos.mySeek.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Entity
@Table(name = "knowledge_documents", indexes = {
        @Index(name = "idx_knowledge_documents_status", columnList = "status"),
        @Index(name = "idx_knowledge_documents_created_at", columnList = "created_at"),
        @Index(name = "idx_knowledge_documents_created_by", columnList = "created_by"),
        @Index(name = "idx_knowledge_documents_user_created", columnList = "created_by, created_at")
})
@Data
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 文档名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 原始文件名
     */
    @Column(name = "original_name")
    private String originalName;

    /**
     * 文档类型 (pdf, doc, docx, txt, md 等)
     */
    @Column(nullable = false)
    private String type;

    /**
     * 文档大小（字节）
     */
    @Column(nullable = false)
    private Long size;

    /**
     * 文档内容（纯文本）
     */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 存储路径
     */
    @Column(name = "storage_path")
    private String storagePath;

    /**
     * 文档状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING;

    /**
     * 处理错误信息
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * 文档分块数量
     */
    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    public enum DocumentStatus {
        PENDING,      // 待处理
        PROCESSING,   // 处理中
        COMPLETED,    // 已完成
        FAILED        // 处理失败
    }
}
