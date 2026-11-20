package top.knos.mySeek.writing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 写作作品：以 JSON 文档保存完整作品树（卷/章节/设定），按用户隔离。
 */
@Entity
@Table(name = "writing_novels", indexes = {
        @Index(name = "idx_writing_novels_user_id", columnList = "user_id")
})
@Data
public class WritingNovel {

    /** 与前端 Novel.id 保持一致，使用客户端生成的 UUID 字符串 */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    /** 完整作品 JSON，包含卷、章节、角色、世界观与大纲 */
    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
