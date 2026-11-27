package top.knos.mySeek.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** 技能（Skill）：可复用的能力单元，本质是 prompt 模板 + 允许使用的工具白名单。 */
@Data
@Entity
@Table(name = "skill_definitions")
public class SkillDefinition {

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

    /** 任务执行时注入的步骤指令模板 */
    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    /** 允许使用的工具名 JSON 数组，空表示不限制 */
    @Column(name = "tools_json", columnDefinition = "TEXT")
    private String toolsJson;

    @Column(name = "builtin")
    private Boolean builtin = false;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
