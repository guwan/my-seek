package top.knos.mySeek.task.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AI 任务中心的顶层任务。用户目标在此持久化，步骤与事件分别关联。
 */
@Data
@Entity
@Table(name = "agent_tasks")
public class AgentTask {

    public enum Status {
        CREATED, PLANNING, PLAN_REVIEW, WAIT_APPROVAL, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, REJECTED
    }

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "goal", length = 4000, nullable = false)
    private String goal;

    @Column(name = "constraints_text", length = 2000)
    private String constraints;

    @Column(name = "model_code", length = 128)
    private String modelCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status;

    @Column(name = "current_step")
    private Integer currentStep = 0;

    @Column(name = "max_steps")
    private Integer maxSteps = 8;

    @Column(name = "final_summary", length = 8000)
    private String finalSummary;

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
