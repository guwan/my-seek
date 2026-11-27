package top.knos.mySeek.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.agent.entity.AgentDefinition;

import java.util.List;
import java.util.Optional;

public interface AgentDefinitionRepository extends JpaRepository<AgentDefinition, String> {

    List<AgentDefinition> findByCreatedByOrderByBuiltinDescCreatedAtAsc(Long createdBy);

    Optional<AgentDefinition> findByCodeAndCreatedBy(String code, Long createdBy);
}
