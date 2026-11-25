package top.knos.mySeek.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.task.entity.AgentTaskEvent;

import java.util.List;
import java.util.Optional;

public interface AgentTaskEventRepository extends JpaRepository<AgentTaskEvent, String> {
    List<AgentTaskEvent> findByTaskIdOrderByCreatedAtAsc(String taskId);

    List<AgentTaskEvent> findByTaskIdAndSeqGreaterThanOrderBySeqAsc(String taskId, Long seq);

    Optional<AgentTaskEvent> findTopByTaskIdOrderBySeqDesc(String taskId);
}
