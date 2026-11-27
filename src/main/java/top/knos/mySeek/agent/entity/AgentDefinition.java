package top.knos.mySeek.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** 专家（Agent）：预置或用户自定义的角色，决定任务规划时的人设与可用技能。 */
@Data
@Entity
@Table(name = "agent_definitions")
public class AgentDefinition {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon", length = 64)
    private String icon;

    /** 规划与执行时注入的人设指令 */
    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    /** 关联技能 code 的 JSON 数组 */
    @Column(name = "skills_json", columnDefinition = "TEXT")
    private String skillsJson;

    /** 推荐模型，留空使用默认 */
    @Column(name = "model_code", length = 128)
    private String modelCode;

    @Column(name = "builtin")
    private Boolean builtin = false;

    @Column(name = "enabled")
    private Boolean enabled = true;

    /** 0 表示系统内置；其余为自定义用户 id */
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
