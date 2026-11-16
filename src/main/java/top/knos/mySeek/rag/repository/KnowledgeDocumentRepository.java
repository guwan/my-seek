package top.knos.mySeek.rag.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.knos.mySeek.rag.entity.KnowledgeDocument;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {

    /**
     * 根据状态查询文档
     */
    List<KnowledgeDocument> findByStatus(KnowledgeDocument.DocumentStatus status);

    /**
     * 根据创建者查询文档
     */
    List<KnowledgeDocument> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    /**
     * 分页查询所有文档（按创建时间倒序）
     */
    Page<KnowledgeDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 分页查询指定状态的文档（按创建时间倒序）
     */
    Page<KnowledgeDocument> findByStatusOrderByCreatedAtDesc(KnowledgeDocument.DocumentStatus status, Pageable pageable);

    /**
     * 查询所有已完成的文档
     */
    List<KnowledgeDocument> findByStatusOrderByCreatedAtDesc(KnowledgeDocument.DocumentStatus status);

    /**
     * 根据名称模糊查询
     */
    List<KnowledgeDocument> findByNameContainingIgnoreCase(String name);
}
