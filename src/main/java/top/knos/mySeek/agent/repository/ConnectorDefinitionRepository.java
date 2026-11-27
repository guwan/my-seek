package top.knos.mySeek.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.agent.entity.ConnectorDefinition;

import java.util.List;
import java.util.Optional;

public interface ConnectorDefinitionRepository extends JpaRepository<ConnectorDefinition, String> {

    List<ConnectorDefinition> findByCreatedByOrderByBuiltinDescCreatedAtAsc(Long createdBy);

    Optional<ConnectorDefinition> findByCodeAndCreatedBy(String code, Long createdBy);
}
