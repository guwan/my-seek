package top.knos.mySeek.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.task.entity.AgentTaskEvent;

import java.util.List;

public interface AgentTaskEventRepository extends JpaRepository<AgentTaskEvent, String> {
    List<AgentTaskEvent> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
