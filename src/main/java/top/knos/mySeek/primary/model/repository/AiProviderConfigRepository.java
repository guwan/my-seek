package top.knos.mySeek.primary.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.knos.mySeek.primary.model.entity.AiProviderConfig;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, String> {

    Optional<AiProviderConfig> findByName(String name);

    boolean existsByName(String name);

    List<AiProviderConfig> findAllByOrderByCreatedAtDesc();
}
