package top.knos.mySeek.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.task.entity.AgentTaskStep;

import java.util.List;

public interface AgentTaskStepRepository extends JpaRepository<AgentTaskStep, String> {
    List<AgentTaskStep> findByTaskIdOrderBySeqAsc(String taskId);
}
