package top.knos.mySeek.rag.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.knos.mySeek.rag.entity.KnowledgeDocument;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {

    /** 分页查询指定用户的文档（按创建时间倒序） */
    Page<KnowledgeDocument> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

    /** 分页查询指定用户、指定状态的文档（按创建时间倒序） */
    Page<KnowledgeDocument> findByCreatedByAndStatusOrderByCreatedAtDesc(
            Long createdBy, KnowledgeDocument.DocumentStatus status, Pageable pageable);

    /** 查询指定用户的文档 */
    List<KnowledgeDocument> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    /** 查询指定用户、指定状态的文档 */
    List<KnowledgeDocument> findByCreatedByAndStatusOrderByCreatedAtDesc(
            Long createdBy, KnowledgeDocument.DocumentStatus status);

    /** 按 ID + 归属用户查询，用于删除/问答的越权校验 */
    Optional<KnowledgeDocument> findByIdAndCreatedBy(String id, Long createdBy);

    /** 查询未进入终态的文档，用于服务启动后的任务恢复 */
    List<KnowledgeDocument> findByStatusIn(Collection<KnowledgeDocument.DocumentStatus> statuses);

    /** 根据名称模糊查询 */
    List<KnowledgeDocument> findByNameContainingIgnoreCase(String name);
}
