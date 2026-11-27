package top.knos.mySeek.task.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 任务执行事件流，供前端展示进度与排查。 */
@Data
@Entity
@Table(name = "agent_task_events")
public class AgentTaskEvent {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /** 任务内递增序号，供 SSE 断点续传与去重 */
    @Column(name = "seq")
    private Long seq = 0L;

    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @Column(name = "step_id", length = 36)
    private String stepId;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
