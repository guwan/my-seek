package top.knos.mySeek.security.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.knos.mySeek.security.domain.User;

import java.util.List;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/14 11:12
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    void deleteByUsername(String username);

    User findByEmail(String email);

    User getUserByEmail(String email);

    List<User> findByNameLike(String keywords, Pageable pageRequest);

    long countBySource(String userFromSj);

    List<User> findByUsernameIn(List<String> sjUserIds);

    User findFirstByUsername(String userName);

    User findFirstByNameAndSource(String name, String userFromSj);

    User findFirstByNameOrderBySourceDesc(String name);

    List<User> findAllByName(String name);

    List<User> findAllByNameAndPasswordIsNotNull(String username);

    /**
     * 分页查询用户并一次性抓取角色集合，避免列表页的 N+1 查询。
     */
    @Override
    @EntityGraph(attributePaths = {"authorities"})
    <S extends User> Page<S> findAll(Example<S> example, Pageable pageable);
}
