/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.knos.mySeek.security.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import top.knos.mySeek.security.domain.Resource;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Integer> {


    @Query("select count(id) from sso_resource where id in (:ids)")
    long countByIds(@Param("ids") List<Integer> ids);
    @Modifying
    @Transactional
    void deleteAllByIdIn(List<Integer> ids);

    List<Resource> findAllByType(int type);

    Resource findFirstByParentIsNullOrderBySortDesc();

    Resource findFirstByParentOrderBySortDesc(Resource parentResource);

    List<Resource> findAllByTypeOrderBySort(int type);

    List<Resource> findAllByParentOrderBySortDesc(Resource resource);

    Resource findFirstByName(String name);

    /*@Query("from sso_resource where id in (select resource_id from sso_authority_resource where authority_id = :id )")
    List<Resource> findAllByAuthorityId(@Param("id") Long id);*/
}
