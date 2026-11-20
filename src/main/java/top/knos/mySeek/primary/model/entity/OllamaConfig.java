package top.knos.mySeek.primary.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 内置 Ollama 运行时配置（单行，id 固定为 default）。
 */
@Entity
@Table(name = "ollama_configs")
@Data
public class OllamaConfig {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    @Column(name = "default_chat_model", nullable = false, length = 128)
    private String defaultChatModel;

    @Column(name = "embedding_model", nullable = false, length = 128)
    private String embeddingModel;

    @Column(name = "pull_model_strategy", nullable = false, length = 32)
    private String pullModelStrategy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
