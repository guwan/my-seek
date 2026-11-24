package top.knos.mySeek.task.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** AI 任务的可执行步骤，对应 WorkBuddy 的 todo 清单。 */
@Data
@Entity
@Table(name = "agent_task_steps")
public class AgentTaskStep {

    public enum Status {
        PENDING, RUNNING, SUCCEEDED, FAILED, SKIPPED
    }

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @Column(name = "seq")
    private Integer seq;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /** model 或白名单工具名，null 视为 model */
    @Column(name = "tool", length = 64)
    private String tool;

    @Column(name = "input_json", length = 4000)
    private String inputJson;

    /** 敏感工具执行前是否需要用户批准 */
    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;

    /** 用户是否已批准该步骤（批准后重试不再重复询问） */
    @Column(name = "approved")
    private Boolean approved = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status;

    @Column(name = "result_text", length = 8000)
    private String resultText;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
