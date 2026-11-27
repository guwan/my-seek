package top.knos.mySeek.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.agent.entity.SkillDefinition;

import java.util.List;
import java.util.Optional;

public interface SkillDefinitionRepository extends JpaRepository<SkillDefinition, String> {

    List<SkillDefinition> findByCreatedByOrderByBuiltinDescCreatedAtAsc(Long createdBy);

    Optional<SkillDefinition> findByCodeAndCreatedBy(String code, Long createdBy);
}
