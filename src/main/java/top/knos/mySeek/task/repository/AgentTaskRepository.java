package top.knos.mySeek.task.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.task.entity.AgentTask;

import java.util.List;
import java.util.Optional;

public interface AgentTaskRepository extends JpaRepository<AgentTask, String> {
    Optional<AgentTask> findByIdAndCreatedBy(String id, Long createdBy);

    Page<AgentTask> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

    List<AgentTask> findByStatusIn(List<AgentTask.Status> statuses);
}
