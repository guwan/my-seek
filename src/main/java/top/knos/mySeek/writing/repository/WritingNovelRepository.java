package top.knos.mySeek.writing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.knos.mySeek.writing.entity.WritingNovel;

import java.util.List;

public interface WritingNovelRepository extends JpaRepository<WritingNovel, String> {

    List<WritingNovel> findByUserIdOrderByCreatedAtDesc(Long userId);
}
