package top.knos.mySeek.primary.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户自定义 AI 接口配置（OpenAI 兼容）。
 */
@Entity
@Table(name = "ai_provider_configs", indexes = {
        @Index(name = "idx_ai_provider_name", columnList = "name", unique = true)
})
@Data
public class AiProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** 提供者标识，作为 model 前缀，如 deepseek；只允许字母数字、下划线、连字符 */
    @Column(nullable = false, unique = true, length = 32)
    private String name;

    /** 显示名称，如 DeepSeek 官方 */
    @Column(nullable = false, length = 64)
    private String displayName;

    /** OpenAI 兼容的 base-url，如 https://api.deepseek.com */
    @Column(nullable = false, length = 512)
    private String baseUrl;

    /** API Key（AES 加密存储） */
    @Column(nullable = false, length = 1024)
    private String apiKey;

    /** 模型列表，JSON 数组字符串 */
    @Column(name = "models_json", columnDefinition = "TEXT")
    private String modelsJson;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
